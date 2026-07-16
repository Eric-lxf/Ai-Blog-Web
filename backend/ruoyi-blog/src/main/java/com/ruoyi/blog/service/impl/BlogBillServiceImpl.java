package com.ruoyi.blog.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.domain.BlogBill;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.dto.BillPageQuery;
import com.ruoyi.blog.dto.BillRecognizeRequest;
import com.ruoyi.blog.dto.BillSaveRequest;
import com.ruoyi.blog.mapper.BlogBillMapper;
import com.ruoyi.blog.service.BlogBillService;
import com.ruoyi.blog.service.DeepSeekService;
import com.ruoyi.blog.service.bill.BillExcelParser;
import com.ruoyi.blog.service.bill.BillPdfRenderer;
import com.ruoyi.blog.vo.BillAnalysisVO;
import com.ruoyi.blog.vo.BillCategoryAmountVO;
import com.ruoyi.blog.vo.BillMonthlyRow;
import com.ruoyi.blog.vo.BillVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogBillServiceImpl implements BlogBillService
{

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*}", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY  = Pattern.compile("\\[.*]",  Pattern.DOTALL);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter TRADE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * OCR 模型更擅长「按表格原文输出」，直接要复杂 JSON 容易只返回第一行且列错位。
     * 固定列顺序与微信交易明细一致，后续在本地解析。
     */
    private static final String TABLE_OCR_PROMPT =
            "请识别图片中的微信支付交易明细/银行流水表格。\n" +
            "只输出 Markdown 表格，不要解释、不要 JSON。\n" +
            "表头必须严格为（顺序不可变）：\n" +
            "|交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|\n" +
            "然后输出分隔行，再输出「具体交易明细」下的每一行数据，禁止只输出第一行。\n" +
            "长数字单号若换行，请拼成完整一串。金额只保留数字和小数点。\n" +
            "示例：\n" +
            "|交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|\n" +
            "|---|---|---|---|---|---|---|---|\n" +
            "|4200003099202607145869|2026-07-14 09:31:21|商户消费|支出|招商银行信用卡(1683)|110.81|通行宝|10001|\n";

    /** 兼容旧 JSON 输出的提示（当表格解析失败时二次调用） */
    private static final String JSON_FALLBACK_PROMPT =
            "上一轮表格识别不完整。请再次识别同一张图，输出 JSON 数组，覆盖全部交易行。\n" +
            "每个元素字段：tradeNo,tradeTime(yyyy-MM-dd HH:mm:ss),billDate,tradeType,direction,merchant,paymentMethod,amount,merchantOrderNo,category,confidence。\n" +
            "列含义：merchant=交易对方，paymentMethod=交易方式（银行卡名+尾号）。只输出 JSON 数组。";

    private final BlogBillMapper billMapper;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    @Override
    public Page<BillVO> page(BillPageQuery query)
    {
        Long userId = SecurityUtils.getUserId();
        int pageNum  = query.getPageNum()  == null || query.getPageNum()  < 1 ? 1  : query.getPageNum();
        int pageSize = query.getPageSize() == null                             ? 10 : Math.min(query.getPageSize(), 100);
        Page<BlogBill> rawPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BlogBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogBill::getUserId, userId);
        if (StringUtils.hasText(query.getCategory()))
        {
            wrapper.eq(BlogBill::getCategory, query.getCategory());
        }
        if (StringUtils.hasText(query.getMerchant()))
        {
            wrapper.like(BlogBill::getMerchant, query.getMerchant());
        }
        if (StringUtils.hasText(query.getDirection()))
        {
            wrapper.eq(BlogBill::getDirection, query.getDirection());
        }
        if (query.getStartDate() != null)
        {
            wrapper.ge(BlogBill::getBillDate, query.getStartDate());
        }
        if (query.getEndDate() != null)
        {
            wrapper.le(BlogBill::getBillDate, query.getEndDate());
        }
        wrapper.orderByDesc(BlogBill::getBillDate, BlogBill::getTradeTime, BlogBill::getCreateTime);
        billMapper.selectPage(rawPage, wrapper);
        Page<BillVO> voPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        voPage.setRecords(rawPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public BillVO getById(Long id)
    {
        return toVO(requireBill(id));
    }

    @Override
    public Long save(BillSaveRequest request)
    {
        Long userId = SecurityUtils.getUserId();
        BlogBill bill;
        if (request.getId() != null)
        {
            bill = requireBill(request.getId());
            if (!bill.getUserId().equals(userId))
            {
                throw new ServiceException("无权限操作该账单", HttpStatus.FORBIDDEN);
            }
        }
        else
        {
            bill = new BlogBill();
            bill.setUserId(userId);
        }
        bill.setBillDate(request.getBillDate());
        bill.setTradeNo(request.getTradeNo());
        bill.setTradeTime(request.getTradeTime());
        bill.setTradeType(request.getTradeType());
        bill.setDirection(StringUtils.hasText(request.getDirection()) ? request.getDirection() : "支出");
        bill.setMerchant(request.getMerchant());
        bill.setCategory(request.getCategory());
        bill.setAmount(request.getAmount());
        bill.setPaymentMethod(request.getPaymentMethod());
        bill.setMerchantOrderNo(request.getMerchantOrderNo());
        bill.setNote(request.getNote());
        bill.setImageUrl(request.getImageUrl());
        bill.setAiConfidence(request.getAiConfidence());
        bill.setSource(request.getSource() != null ? request.getSource() : 0);
        if (bill.getId() == null)
        {
            billMapper.insert(bill);
        }
        else
        {
            billMapper.updateById(bill);
        }
        return bill.getId();
    }

    @Override
    public void delete(Long id)
    {
        BlogBill bill = requireBill(id);
        if (!bill.getUserId().equals(SecurityUtils.getUserId()))
        {
            throw new ServiceException("无权限操作该账单", HttpStatus.FORBIDDEN);
        }
        billMapper.deleteById(id);
    }

    @Override
    public List<BillVO> recognize(BillRecognizeRequest request)
    {
        return recognizeImageUrl(validateRecognizeImageUrl(request.getImageUrl()));
    }

    @Override
    public List<BillVO> recognizeFile(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("请上传图片、PDF 或 Excel 文件", HttpStatus.BAD_REQUEST);
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        try
        {
            if (isExcel(filename, contentType))
            {
                List<BillVO> list = BillExcelParser.parse(file.getInputStream());
                if (list.isEmpty())
                {
                    throw new ServiceException("Excel 中未解析到账单明细，请确认表头含金额与日期列", HttpStatus.BAD_REQUEST);
                }
                return list;
            }
            if (isPdf(filename, contentType))
            {
                List<String> pages = BillPdfRenderer.toJpegDataUrls(file.getInputStream());
                List<BillVO> all = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++)
                {
                    log.info("Bill PDF OCR page {}/{}", i + 1, pages.size());
                    all.addAll(recognizeImageUrl(pages.get(i)));
                }
                if (all.isEmpty())
                {
                    throw new ServiceException("未能从 PDF 中识别到账单明细", HttpStatus.ERROR);
                }
                return all;
            }
            if (isImage(filename, contentType))
            {
                String dataUrl = toImageDataUrl(file.getBytes(), filename, contentType);
                return recognizeImageUrl(dataUrl);
            }
            throw new ServiceException("仅支持图片（JPG/PNG/WEBP）、PDF、Excel（xls/xlsx）", HttpStatus.BAD_REQUEST);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("recognizeFile failed name={}", filename, e);
            throw new ServiceException("文件识别失败：" + e.getMessage(), HttpStatus.ERROR);
        }
    }

    private List<BillVO> recognizeImageUrl(String imageUrl)
    {
        String tableRaw = deepSeekService.recognizeImage(imageUrl, TABLE_OCR_PROMPT, AiModuleCode.BILL_VISION);
        log.info("Bill OCR table raw len={} preview={}", tableRaw != null ? tableRaw.length() : 0,
                abbreviate(tableRaw, 400));

        List<BillVO> list = parseMarkdownTable(tableRaw);
        if (list.size() <= 1)
        {
            // 表格解析不足时：尝试把同一次输出当 JSON；仍不足则二次调用强制 JSON 全量
            List<BillVO> fromJson = parseRecognizeResults(tableRaw, objectMapper);
            if (fromJson.size() > list.size())
            {
                list = fromJson;
            }
        }
        if (list.size() <= 1)
        {
            String jsonRaw = deepSeekService.recognizeImage(imageUrl, JSON_FALLBACK_PROMPT, AiModuleCode.BILL_VISION);
            log.info("Bill OCR json fallback len={} preview={}", jsonRaw != null ? jsonRaw.length() : 0,
                    abbreviate(jsonRaw, 400));
            List<BillVO> fromJson = parseRecognizeResults(jsonRaw, objectMapper);
            if (fromJson.size() > list.size())
            {
                list = fromJson;
            }
        }
        if (list.isEmpty())
        {
            throw new ServiceException("未能从图片中识别到账单明细，请换更清晰的原图后重试", HttpStatus.ERROR);
        }
        return list;
    }

    private static boolean isExcel(String filename, String contentType)
    {
        return filename.endsWith(".xlsx") || filename.endsWith(".xls")
                || contentType.contains("spreadsheet") || contentType.contains("excel");
    }

    private static boolean isPdf(String filename, String contentType)
    {
        return filename.endsWith(".pdf") || contentType.contains("pdf");
    }

    private static boolean isImage(String filename, String contentType)
    {
        return contentType.startsWith("image/")
                || filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".webp") || filename.endsWith(".gif");
    }

    private static String toImageDataUrl(byte[] bytes, String filename, String contentType)
    {
        String mime = contentType.startsWith("image/") ? contentType : guessImageMime(filename);
        return "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private static String guessImageMime(String filename)
    {
        if (filename.endsWith(".png"))
        {
            return "image/png";
        }
        if (filename.endsWith(".webp"))
        {
            return "image/webp";
        }
        if (filename.endsWith(".gif"))
        {
            return "image/gif";
        }
        return "image/jpeg";
    }

    /**
     * 远端视觉/OCR（如 DashScope qwen3.5-ocr）只能拉取公网 URL 或 data:image Base64，
     * 浏览器 blob:/file: 地址不可达，会表现为「AI 服务暂时不可用」。
     */
    public static String validateRecognizeImageUrl(String imageUrl)
    {
        if (!StringUtils.hasText(imageUrl))
        {
            throw new ServiceException("图片地址不能为空", HttpStatus.BAD_REQUEST);
        }
        String url = imageUrl.trim();
        if (url.startsWith("blob:") || url.startsWith("file:"))
        {
            throw new ServiceException("图片地址无效：请重新上传本地图片（将自动转为 Base64），或粘贴可公网访问的图片 URL",
                    HttpStatus.BAD_REQUEST);
        }
        if (url.startsWith("data:image/") || url.startsWith("http://") || url.startsWith("https://"))
        {
            return url;
        }
        throw new ServiceException("图片地址无效：仅支持 http(s) URL 或 Base64 图片（data:image/...）",
                HttpStatus.BAD_REQUEST);
    }

    @Override
    public BillAnalysisVO analysis(int months)
    {
        if (months <= 0 || months > 12) months = 6;
        Long userId = SecurityUtils.getUserId();
        LocalDate endDate   = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months - 1L).withDayOfMonth(1);

        List<BillCategoryAmountVO> categoryPie = billMapper.selectCategoryTotals(userId, startDate, endDate);
        List<BillMonthlyRow>       monthlyRows = billMapper.selectMonthlyTotals(userId, startDate, endDate);
        List<String>               monthsList  = buildMonthsList(startDate, endDate);

        BigDecimal total = categoryPie.stream()
                .map(BillCategoryAmountVO::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        String topCategory = categoryPie.isEmpty() ? "暂无数据" : categoryPie.get(0).getName();
        long billCount = billMapper.selectCount(new LambdaQueryWrapper<BlogBill>()
                .eq(BlogBill::getUserId, userId)
                .ge(BlogBill::getBillDate, startDate)
                .le(BlogBill::getBillDate, endDate)
                .and(w -> w.isNull(BlogBill::getDirection)
                        .or().eq(BlogBill::getDirection, "")
                        .or().eq(BlogBill::getDirection, "支出")));

        BillAnalysisVO vo = new BillAnalysisVO();
        vo.setSummary(new BillAnalysisVO.SummaryData(total, avg, billCount, topCategory));
        vo.setCategoryPie(categoryPie);
        vo.setMonths(monthsList);
        vo.setMonthlyTrend(buildMonthlySeries(monthlyRows, monthsList));
        vo.setAiAdvice(generateAdvice(categoryPie, total, months));
        return vo;
    }

    // ── private helpers ──────────────────────────────────────────

    private BlogBill requireBill(Long id)
    {
        BlogBill bill = billMapper.selectById(id);
        if (bill == null) throw new ServiceException("账单不存在", HttpStatus.NOT_FOUND);
        return bill;
    }

    private BillVO toVO(BlogBill bill)
    {
        BillVO vo = new BillVO();
        BeanUtils.copyProperties(bill, vo);
        vo.setSourceName(bill.getSource() != null && bill.getSource() == 1 ? "AI识别" : "手动录入");
        return vo;
    }

    /**
     * 解析 OCR 输出的 Markdown 表格。列顺序固定为微信明细：
     * 交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号
     */
    public static List<BillVO> parseMarkdownTable(String raw)
    {
        List<BillVO> list = new ArrayList<>();
        if (!StringUtils.hasText(raw))
        {
            return list;
        }
        String[] lines = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines)
        {
            String trimmed = line.trim();
            if (!trimmed.contains("|"))
            {
                continue;
            }
            if (isMarkdownSeparatorOrHeader(trimmed))
            {
                continue;
            }
            List<String> cells = splitMarkdownRow(trimmed);
            if (cells.size() < 6)
            {
                continue;
            }
            // 允许 7~8+ 列；不足 8 列时按已有列填充
            while (cells.size() < 8)
            {
                cells.add("");
            }
            BillVO vo = fromTableCells(cells);
            if (vo != null)
            {
                list.add(vo);
            }
        }
        return list;
    }

    private static boolean isMarkdownSeparatorOrHeader(String line)
    {
        String compact = line.replace("|", "").replace("-", "").replace(":", "").replace(" ", "");
        if (!StringUtils.hasText(compact))
        {
            return true;
        }
        return line.contains("交易单号") && line.contains("交易时间");
    }

    private static List<String> splitMarkdownRow(String line)
    {
        String normalized = line;
        if (normalized.startsWith("|"))
        {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("|"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        String[] parts = normalized.split("\\|", -1);
        List<String> cells = new ArrayList<>(parts.length);
        for (String part : parts)
        {
            cells.add(part.trim().replace('\u00A0', ' '));
        }
        return cells;
    }

    private static BillVO fromTableCells(List<String> cells)
    {
        String tradeNo = cells.get(0);
        String tradeTime = cells.get(1);
        String tradeType = cells.get(2);
        String direction = cells.get(3);
        String paymentMethod = cells.get(4);
        String amountStr = cells.get(5);
        String merchant = cells.get(6);
        String merchantOrderNo = cells.get(7);

        BigDecimal amount = parseAmountText(amountStr);
        if (amount == null && !StringUtils.hasText(merchant) && !StringUtils.hasText(tradeNo))
        {
            return null;
        }
        // 跳过明显非数据行
        if ("金额".equals(amountStr) || "交易对方".equals(merchant))
        {
            return null;
        }

        BillVO vo = new BillVO();
        vo.setTradeNo(blankToNull(tradeNo));
        vo.setTradeType(blankToNull(tradeType));
        vo.setDirection(StringUtils.hasText(direction) ? direction : "支出");
        vo.setPaymentMethod(blankToNull(paymentMethod));
        vo.setAmount(amount);
        vo.setMerchant(blankToNull(merchant));
        vo.setMerchantOrderNo(blankToNull(merchantOrderNo));
        vo.setNote(blankToNull(tradeType));
        applyTradeTime(vo, tradeTime);
        if (vo.getBillDate() == null && StringUtils.hasText(tradeTime) && tradeTime.length() >= 10)
        {
            try
            {
                vo.setBillDate(LocalDate.parse(tradeTime.substring(0, 10)));
            }
            catch (Exception ignored)
            {
            }
        }
        vo.setCategory(guessCategory(merchant, tradeType));
        vo.setAiConfidence(85);
        vo.setSource(1);
        vo.setSourceName("AI识别");
        return vo;
    }

    private static void applyTradeTime(BillVO vo, String tradeTimeStr)
    {
        if (!StringUtils.hasText(tradeTimeStr))
        {
            return;
        }
        try
        {
            String normalized = tradeTimeStr.replace('T', ' ').trim();
            if (normalized.length() >= 19)
            {
                vo.setTradeTime(LocalDateTime.parse(normalized.substring(0, 19), TRADE_TIME_FMT));
                vo.setBillDate(vo.getTradeTime().toLocalDate());
            }
            else if (normalized.length() >= 16)
            {
                vo.setTradeTime(LocalDateTime.parse(normalized.substring(0, 16),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                vo.setBillDate(vo.getTradeTime().toLocalDate());
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static String blankToNull(String v)
    {
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private static BigDecimal parseAmountText(String amtStr)
    {
        if (!StringUtils.hasText(amtStr))
        {
            return null;
        }
        try
        {
            String cleaned = amtStr.replaceAll("[^\\d.]", "");
            if (!StringUtils.hasText(cleaned))
            {
                return null;
            }
            return new BigDecimal(cleaned);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 解析 AI 返回：优先完整 JSON 数组；截断时按平衡括号提取多个对象；最后兼容单对象。
     * 也支持 {"items":[...]} / {"transactions":[...]} / {"rows":[...]} 包装。
     */
    public static List<BillVO> parseRecognizeResults(String raw, ObjectMapper mapper)
    {
        List<BillVO> list = new ArrayList<>();
        if (!StringUtils.hasText(raw) || mapper == null)
        {
            return list;
        }
        try
        {
            String text = stripMarkdownFence(raw);
            Matcher arrayMatcher = JSON_ARRAY.matcher(text);
            if (arrayMatcher.find())
            {
                try
                {
                    JsonNode arr = mapper.readTree(arrayMatcher.group());
                    if (arr.isArray())
                    {
                        addRecognizeNodes(list, arr);
                        if (!list.isEmpty())
                        {
                            return list;
                        }
                    }
                }
                catch (Exception parseArrayEx)
                {
                    log.warn("完整 JSON 数组解析失败，尝试按对象片段恢复：{}", parseArrayEx.getMessage());
                    list.addAll(parseObjectFragments(arrayMatcher.group(), mapper));
                    if (!list.isEmpty())
                    {
                        return list;
                    }
                }
            }

            // 截断数组（无结尾 ]）时，Jackson 读单对象会只拿到第一笔；优先按平衡括号提取多对象
            if (text.indexOf('{') != text.lastIndexOf('{'))
            {
                list.addAll(parseObjectFragments(text, mapper));
                if (list.size() > 1)
                {
                    return list;
                }
                list.clear();
            }

            Matcher objectMatcher = JSON_OBJECT.matcher(text);
            if (objectMatcher.find())
            {
                try
                {
                    JsonNode node = mapper.readTree(objectMatcher.group());
                    if (node.isObject())
                    {
                        JsonNode nested = firstArrayChild(node, "items", "transactions", "rows", "bills", "data", "list");
                        if (nested != null)
                        {
                            addRecognizeNodes(list, nested);
                            if (!list.isEmpty())
                            {
                                return list;
                            }
                        }
                        BillVO vo = toRecognizeVo(node);
                        if (vo != null)
                        {
                            list.add(vo);
                        }
                    }
                }
                catch (Exception objectEx)
                {
                    list.addAll(parseObjectFragments(text, mapper));
                }
            }
            else
            {
                list.addAll(parseObjectFragments(text, mapper));
            }
            if (list.isEmpty())
            {
                log.warn("AI 识别结果无法解析为 JSON：{}", abbreviate(raw, 500));
            }
        }
        catch (Exception e)
        {
            log.warn("解析 AI 识别 JSON 失败：{}", abbreviate(raw, 500), e);
            list.addAll(parseObjectFragments(raw, mapper));
        }
        return list;
    }

    private static void addRecognizeNodes(List<BillVO> list, JsonNode arr)
    {
        for (JsonNode node : arr)
        {
            BillVO vo = toRecognizeVo(node);
            if (vo != null)
            {
                list.add(vo);
            }
        }
    }

    private static JsonNode firstArrayChild(JsonNode node, String... fields)
    {
        for (String field : fields)
        {
            JsonNode child = node.get(field);
            if (child != null && child.isArray() && !child.isEmpty())
            {
                return child;
            }
        }
        return null;
    }

    /** 从可能被截断的文本中提取多个完整 {...} JSON 对象。 */
    static List<BillVO> parseObjectFragments(String raw, ObjectMapper mapper)
    {
        List<BillVO> list = new ArrayList<>();
        if (!StringUtils.hasText(raw) || mapper == null)
        {
            return list;
        }
        int i = 0;
        while (i < raw.length())
        {
            int start = raw.indexOf('{', i);
            if (start < 0)
            {
                break;
            }
            int end = findMatchingBrace(raw, start);
            if (end < 0)
            {
                break;
            }
            String fragment = raw.substring(start, end + 1);
            try
            {
                BillVO vo = toRecognizeVo(mapper.readTree(fragment));
                if (vo != null)
                {
                    list.add(vo);
                }
            }
            catch (Exception ignored)
            {
                // skip broken fragment
            }
            i = end + 1;
        }
        return list;
    }

    private static int findMatchingBrace(String text, int start)
    {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (inString)
            {
                if (c == '\\' && i + 1 < text.length())
                {
                    i++;
                    continue;
                }
                if (c == '"')
                {
                    inString = false;
                }
                continue;
            }
            if (c == '"')
            {
                inString = true;
            }
            else if (c == '{')
            {
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String stripMarkdownFence(String raw)
    {
        String text = raw.trim();
        if (text.startsWith("```"))
        {
            int firstNl = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl)
            {
                return text.substring(firstNl + 1, lastFence).trim();
            }
        }
        return text;
    }

    private static String abbreviate(String text, int max)
    {
        if (text == null || text.length() <= max)
        {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    private static BillVO toRecognizeVo(JsonNode node)
    {
        if (node == null || !node.isObject())
        {
            return null;
        }
        BillVO vo = new BillVO();
        vo.setTradeNo(textOrNull(node, "tradeNo"));
        vo.setTradeType(textOrNull(node, "tradeType"));
        vo.setDirection(textOrNull(node, "direction"));
        vo.setMerchantOrderNo(textOrNull(node, "merchantOrderNo"));

        String tradeTimeStr = textOrNull(node, "tradeTime");
        if (StringUtils.hasText(tradeTimeStr))
        {
            try
            {
                String normalized = tradeTimeStr.replace('T', ' ').trim();
                if (normalized.length() >= 19)
                {
                    vo.setTradeTime(LocalDateTime.parse(normalized.substring(0, 19),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                else if (normalized.length() >= 16)
                {
                    vo.setTradeTime(LocalDateTime.parse(normalized.substring(0, 16),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                }
            }
            catch (Exception ignored)
            {
            }
        }

        String dateStr = node.path("billDate").asText("");
        if (!StringUtils.hasText(dateStr) && vo.getTradeTime() != null)
        {
            dateStr = vo.getTradeTime().toLocalDate().toString();
        }
        if (StringUtils.hasText(dateStr))
        {
            String normalized = dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr;
            try
            {
                vo.setBillDate(LocalDate.parse(normalized));
            }
            catch (Exception ignored)
            {
            }
        }
        if (vo.getBillDate() == null && vo.getTradeTime() != null)
        {
            vo.setBillDate(vo.getTradeTime().toLocalDate());
        }

        String merchant = textOrNull(node, "merchant");
        if (!StringUtils.hasText(merchant))
        {
            merchant = textOrNull(node, "counterparty");
        }
        vo.setMerchant(merchant);
        vo.setCategory(textOrNull(node, "category"));
        BigDecimal amount = parseAmount(node.get("amount"));
        vo.setAmount(amount);
        vo.setPaymentMethod(textOrNull(node, "paymentMethod"));
        String note = textOrNull(node, "note");
        if (!StringUtils.hasText(note) && StringUtils.hasText(vo.getTradeType()))
        {
            note = vo.getTradeType();
        }
        vo.setNote(note);
        int conf = node.path("confidence").asInt(0);
        vo.setAiConfidence(conf > 0 ? conf : null);
        vo.setSource(1);
        vo.setSourceName("AI识别");
        if (!StringUtils.hasText(vo.getDirection()))
        {
            vo.setDirection("支出");
        }
        if (!StringUtils.hasText(vo.getCategory()))
        {
            vo.setCategory(guessCategory(merchant, vo.getTradeType()));
        }
        if (amount == null && !StringUtils.hasText(merchant) && !StringUtils.hasText(vo.getTradeNo()))
        {
            return null;
        }
        return vo;
    }

    private static String guessCategory(String merchant, String tradeType)
    {
        String m = merchant == null ? "" : merchant;
        String t = tradeType == null ? "" : tradeType;
        if (m.contains("通行宝") || m.contains("地铁") || m.contains("滴滴") || m.contains("高德") || m.contains("ETC"))
        {
            return "交通出行";
        }
        if (t.contains("转账"))
        {
            return "其他";
        }
        return "其他";
    }

    private static String textOrNull(JsonNode node, String field)
    {
        String v = node.path(field).asText(null);
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    private static BigDecimal parseAmount(JsonNode amountNode)
    {
        if (amountNode == null || amountNode.isNull())
        {
            return null;
        }
        try
        {
            if (amountNode.isNumber())
            {
                return amountNode.decimalValue();
            }
            String amtStr = amountNode.asText("");
            if (!StringUtils.hasText(amtStr))
            {
                return null;
            }
            return new BigDecimal(amtStr.replaceAll("[^\\d.]", ""));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private List<String> buildMonthsList(LocalDate startDate, LocalDate endDate)
    {
        List<String> list = new ArrayList<>();
        YearMonth cur = YearMonth.from(startDate), end = YearMonth.from(endDate);
        while (!cur.isAfter(end)) { list.add(cur.format(MONTH_FMT)); cur = cur.plusMonths(1); }
        return list;
    }

    private List<BillAnalysisVO.MonthlySeriesItem> buildMonthlySeries(
            List<BillMonthlyRow> rows, List<String> months)
    {
        Map<String, Map<String, BigDecimal>> map = new LinkedHashMap<>();
        for (BillMonthlyRow row : rows)
        {
            map.computeIfAbsent(row.getCategory(), k -> new LinkedHashMap<>())
               .put(row.getMonth(), row.getAmount());
        }
        List<BillAnalysisVO.MonthlySeriesItem> series = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> e : map.entrySet())
        {
            List<BigDecimal> data = months.stream()
                    .map(m -> e.getValue().getOrDefault(m, BigDecimal.ZERO)).toList();
            series.add(new BillAnalysisVO.MonthlySeriesItem(e.getKey(), data));
        }
        return series;
    }

    private List<BillAnalysisVO.AdviceItem> generateAdvice(
            List<BillCategoryAmountVO> categories, BigDecimal total, int months)
    {
        if (categories.isEmpty()) return List.of();
        try
        {
            BigDecimal avg = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            String topJson = categories.stream().limit(5)
                    .map(c -> "\"" + c.getName() + "\": " + c.getValue().toPlainString())
                    .collect(Collectors.joining(", ", "{", "}"));
            String prompt =
                    "用户过去 " + months + " 个月消费分类汇总（元）：" + topJson + "\n" +
                    "总消费：" + total.toPlainString() + " 元，月均：" + avg.toPlainString() + " 元。\n" +
                    "请给出 3-4 条简洁的个性化理财建议，以 JSON 数组返回：\n" +
                    "[{\"tone\":\"warning|info|success|danger\",\"title\":\"标题（10字内）\",\"detail\":\"建议内容（60字内）\"}]\n" +
                    "只输出 JSON 数组，不要其他文字。";
            AiCompletionRequest req = new AiCompletionRequest();
            req.setScene("BILL_ADVICE");
            req.setPrompt(prompt);
            String raw = deepSeekService.chatCompletion(req, AiModuleCode.BILL_ADVICE);
            Matcher m = JSON_ARRAY.matcher(raw);
            if (!m.find()) return List.of();
            return objectMapper.readValue(m.group(), new TypeReference<List<BillAnalysisVO.AdviceItem>>() {});
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e) { log.warn("生成账单建议失败", e); return List.of(); }
    }
}