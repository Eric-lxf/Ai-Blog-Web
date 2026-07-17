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
    private static final Pattern DATE_TIME = Pattern.compile(
            "^\\d{4}[-/年.]\\d{1,2}[-/月.]\\d{1,2}([\\sT]\\d{1,2}:\\d{2}(:\\d{2})?)?$");
    private static final Pattern AMOUNT_CELL = Pattern.compile("^[-+]?[¥￥]?\\d{1,7}(\\.\\d{1,2})?元?$");
    private static final Pattern LONG_DIGITS = Pattern.compile("^\\d{10,}$");
    /** 仅匹配明确的页脚/链接行；勿用「客服/投诉/.cn」等过宽关键字，否则会误杀正常交易行 */
    private static final Pattern JUNK_ROW = Pattern.compile(
            "(?i)https?://|www\\.|加载更多|查看详情|查看更多|帮助中心|意见反馈|微信支付团队|点击这里|第\\s*\\d+\\s*页");
    private static final Pattern HAS_AMOUNT_HINT = Pattern.compile("\\d+\\.\\d{1,2}");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter TRADE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * OCR 主提示：从表头开始识别；仅「横线+竖线」构成的表格行才输出。
     * 固定列顺序与微信交易明细一致，本地再解析。
     */
    private static final String TABLE_OCR_PROMPT =
            "从图中「交易明细表格」的表头行开始识别，只输出该表格。\n" +
            "行判定：同时具备横线与竖线的才是表格行；仅有文字、无横线或无竖线的内容一律跳过（含说明文字、页眉页脚、链接、按钮、页码等）。\n" +
            "只输出 Markdown 表格（半角 |），不要解释、不要 JSON、不要表外任何文字。\n" +
            "表头固定为（顺序不可变）：\n" +
            "|交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|\n" +
            "|---|---|---|---|---|---|---|---|\n" +
            "随后输出表头之下每一行明细；空列用 || 占位，禁止后列前移。\n" +
            "无交易时间/日期的行不要输出。长数字单号换行请拼成一串；金额只保留数字和小数点。";

    /** 表格解析不足时的二次识别提示（同一轮内的格式兜底，不算「重识别轮」） */
    private static final String JSON_FALLBACK_PROMPT =
            "请再次识别同一张图中的交易明细表格（从表头行开始；仅横线+竖线同时存在的行）。\n" +
            "输出 JSON 数组，字段：tradeNo,tradeTime(yyyy-MM-dd HH:mm:ss),billDate,tradeType,direction,merchant,paymentMethod,amount,merchantOrderNo,category,confidence。\n" +
            "无日期的行不要输出；空列勿前移；忽略表外说明与页脚。只输出 JSON 数组。";

    /** 解析后自检不合格时，最多再整轮识别 1 次 */
    private static final String REPAIR_OCR_PROMPT =
            "上一轮识别结果不完整或格式有误，请重新识别本图交易明细表格。\n" +
            "从表头开始；仅横线+竖线同时存在的行；无日期行不要输出。\n" +
            "只输出 Markdown 表格（半角 |），表头固定：\n" +
            "|交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|\n" +
            "|---|---|---|---|---|---|---|---|\n" +
            "每行必须含有效交易时间与金额；空列用 ||；禁止后列前移；不要表外文字。";

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
        deleteByIds(new Long[] { id });
    }

    @Override
    public void deleteByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的账单", HttpStatus.BAD_REQUEST);
        }
        Long userId = SecurityUtils.getUserId();
        for (Long id : ids)
        {
            if (id == null)
            {
                continue;
            }
            BlogBill bill = requireBill(id);
            if (!bill.getUserId().equals(userId))
            {
                throw new ServiceException("无权限操作该账单", HttpStatus.FORBIDDEN);
            }
            billMapper.deleteById(id);
        }
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
                // PDF：按页各渲染一张图，再逐页 OCR（互不拼页）
                List<String> pages = BillPdfRenderer.toJpegDataUrls(file.getInputStream());
                List<BillVO> all = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++)
                {
                    log.info("Bill PDF OCR page {}/{} (one image per page)", i + 1, pages.size());
                    try
                    {
                        all.addAll(recognizeImageUrl(pages.get(i)));
                    }
                    catch (ServiceException pageEx)
                    {
                        // 单页无明细时跳过，继续后续页
                        log.warn("Bill PDF page {}/{} skipped: {}", i + 1, pages.size(), pageEx.getMessage());
                    }
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
        List<BillVO> list = recognizeImageOnce(imageUrl, TABLE_OCR_PROMPT, true);
        String qualityIssue = findRecognizeQualityIssue(list);
        if (qualityIssue != null)
        {
            // 自检不合格：最多再整轮识别 1 次，禁止死循环
            log.warn("Bill OCR quality check failed ({}), retry recognize once", qualityIssue);
            List<BillVO> repaired = recognizeImageOnce(imageUrl, REPAIR_OCR_PROMPT, true);
            String repairedIssue = findRecognizeQualityIssue(repaired);
            if (repairedIssue == null || (!repaired.isEmpty() && repaired.size() >= list.size()))
            {
                list = repaired;
                qualityIssue = repairedIssue;
            }
            if (qualityIssue != null)
            {
                log.warn("Bill OCR still imperfect after 1 repair round: {}", qualityIssue);
            }
        }
        if (list.isEmpty())
        {
            throw new ServiceException("未能从图片中识别到账单明细，请换更清晰的原图后重试", HttpStatus.ERROR);
        }
        return list;
    }

    /**
     * 单轮识别：表格 OCR → 必要时同轮 JSON 兜底。不算「重识别轮」。
     */
    private List<BillVO> recognizeImageOnce(String imageUrl, String tablePrompt, boolean allowJsonFallback)
    {
        String tableRaw = deepSeekService.recognizeImage(imageUrl, tablePrompt, AiModuleCode.BILL_VISION);
        log.info("Bill OCR table raw len={} preview={}", tableRaw != null ? tableRaw.length() : 0,
                abbreviate(tableRaw, 400));

        List<BillVO> list = parseMarkdownTable(tableRaw);
        log.info("Bill OCR table parsed rows={}", list.size());
        if (list.size() <= 1)
        {
            List<BillVO> fromJson = parseRecognizeResults(tableRaw, objectMapper);
            if (fromJson.size() > list.size())
            {
                list = fromJson;
            }
        }
        if (allowJsonFallback && list.size() <= 1)
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
        return list;
    }

    /**
     * 自检识别结果是否格式正确且完整。返回 null 表示通过，否则为问题描述。
     */
    public static String findRecognizeQualityIssue(List<BillVO> list)
    {
        if (list == null || list.isEmpty())
        {
            return "结果为空";
        }
        for (int i = 0; i < list.size(); i++)
        {
            String rowIssue = findRowQualityIssue(list.get(i));
            if (rowIssue != null)
            {
                return "第" + (i + 1) + "行" + rowIssue;
            }
        }
        return null;
    }

    /** 单行是否具备可用账单字段：日期 + 金额 +（对方或单号） */
    public static String findRowQualityIssue(BillVO vo)
    {
        if (vo == null)
        {
            return "为空";
        }
        if (vo.getBillDate() == null && vo.getTradeTime() == null)
        {
            return "缺少日期";
        }
        if (vo.getAmount() == null || vo.getAmount().signum() <= 0)
        {
            return "金额无效";
        }
        if (!StringUtils.hasText(vo.getMerchant()) && !StringUtils.hasText(vo.getTradeNo()))
        {
            return "缺少交易对方与交易单号";
        }
        // 明显列错位：交易类型位像日期、或对方位像金额
        if (StringUtils.hasText(vo.getTradeType()) && isDateTimeCell(vo.getTradeType()))
        {
            return "交易类型疑似日期（列错位）";
        }
        if (StringUtils.hasText(vo.getMerchant()) && isAmountCell(vo.getMerchant()))
        {
            return "交易对方疑似金额（列错位）";
        }
        return null;
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
     * <p>空列保留为空，不向前借用上一行；OCR 漏掉空列竖线时按字段类型重对齐。
     */
    public static List<BillVO> parseMarkdownTable(String raw)
    {
        List<BillVO> list = parseMarkdownTableInternal(raw, true);
        // 过滤/重对齐后若为空，回退到宽松解析，避免「一张都识别不到」
        if (list.isEmpty())
        {
            list = parseMarkdownTableInternal(raw, false);
        }
        return list;
    }

    private static List<BillVO> parseMarkdownTableInternal(String raw, boolean strict)
    {
        List<BillVO> list = new ArrayList<>();
        if (!StringUtils.hasText(raw))
        {
            return list;
        }
        // OCR 常输出全角竖线 ｜ / 盒线 │，统一成半角后再拆列
        String normalizedRaw = raw.replace('\uFF5C', '|').replace('\u2502', '|')
                .replace("｜", "|").replace("│", "|");
        String[] lines = normalizedRaw.replace("\r\n", "\n").replace('\r', '\n').split("\n");
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
            if (strict && isJunkRowText(trimmed) && !HAS_AMOUNT_HINT.matcher(trimmed).find())
            {
                continue;
            }
            List<String> cells = splitMarkdownRow(trimmed);
            if (cells.isEmpty())
            {
                continue;
            }
            // 宽松模式：仅按位置补齐，避免错误重排把金额冲掉
            List<String> aligned = strict ? alignWechatColumns(cells) : padOrTrimTo8(cells);
            BillVO vo = fromTableCells(aligned, strict);
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

    private static boolean isJunkRowText(String text)
    {
        if (!StringUtils.hasText(text))
        {
            return false;
        }
        return JUNK_ROW.matcher(text).find();
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

    /**
     * 对齐到 8 列微信明细。若 OCR 用 || 保留了空列则信任位置；若漏掉空列导致「往后列前推」则按类型重排。
     */
    static List<String> alignWechatColumns(List<String> rawCells)
    {
        List<String> cells = new ArrayList<>(rawCells);
        // 去掉首尾因多余 | 产生的空单元，但保留中间空列
        while (!cells.isEmpty() && !StringUtils.hasText(cells.get(0)) && cells.size() > 8)
        {
            cells.remove(0);
        }
        while (!cells.isEmpty() && !StringUtils.hasText(cells.get(cells.size() - 1)) && cells.size() > 8)
        {
            cells.remove(cells.size() - 1);
        }

        boolean hasInternalEmpty = false;
        for (int i = 0; i < cells.size(); i++)
        {
            if (!StringUtils.hasText(cells.get(i)) && i > 0 && i < cells.size() - 1)
            {
                hasInternalEmpty = true;
                break;
            }
        }

        if ((cells.size() >= 7 || hasInternalEmpty) && looksPositionallyAligned(cells))
        {
            return padOrTrimTo8(cells);
        }
        // 列数不对或位置类型明显错位：按字段类型重排，空槽保持空（不向前借用）
        return realignByFieldType(cells);
    }

    private static List<String> padOrTrimTo8(List<String> cells)
    {
        List<String> out = new ArrayList<>(8);
        for (int i = 0; i < 8; i++)
        {
            out.add(i < cells.size() ? nullToEmpty(cells.get(i)) : "");
        }
        return out;
    }

    private static String nullToEmpty(String v)
    {
        return v == null ? "" : v;
    }

    private static boolean looksPositionallyAligned(List<String> cells)
    {
        List<String> padded = padOrTrimTo8(cells);
        String time = padded.get(1);
        String direction = padded.get(3);
        String amount = padded.get(5);
        if (StringUtils.hasText(time) && !isDateTimeCell(time) && isTradeTypeCell(time))
        {
            return false;
        }
        if (StringUtils.hasText(direction) && !isDirectionCell(direction) && isAmountCell(direction))
        {
            return false;
        }
        if (StringUtils.hasText(amount) && !isAmountCell(amount) && (isDateTimeCell(amount) || isTradeTypeCell(amount)))
        {
            return false;
        }
        // 时间位误放了交易类型/方式 → 典型「空列被前推」
        if (StringUtils.hasText(time) && !isDateTimeCell(time) && (isPaymentCell(time) || isDirectionCell(time)))
        {
            return false;
        }
        return true;
    }

    /**
     * 将可能缺空列的单元格按类型放入固定 8 槽；未识别到的槽保持空，绝不拿上一行补。
     */
    static List<String> realignByFieldType(List<String> cells)
    {
        String[] slots = new String[] {"", "", "", "", "", "", "", ""};
        boolean[] used = new boolean[8];
        for (String raw : cells)
        {
            String cell = nullToEmpty(raw).trim();
            if (!StringUtils.hasText(cell))
            {
                continue;
            }
            int idx = suggestSlot(cell, used);
            if (idx < 0)
            {
                // 无法归类时优先落到交易对方，避免挤进日期/金额
                idx = !used[6] ? 6 : firstFree(used);
            }
            if (idx >= 0)
            {
                slots[idx] = cell;
                used[idx] = true;
            }
        }
        List<String> out = new ArrayList<>(8);
        for (String slot : slots)
        {
            out.add(slot);
        }
        return out;
    }

    private static int suggestSlot(String cell, boolean[] used)
    {
        if (isDateTimeCell(cell) && !used[1])
        {
            return 1;
        }
        if (isDirectionCell(cell) && !used[3])
        {
            return 3;
        }
        if (isAmountCell(cell) && !used[5])
        {
            return 5;
        }
        if (isTradeTypeCell(cell) && !used[2])
        {
            return 2;
        }
        if (isPaymentCell(cell) && !used[4])
        {
            return 4;
        }
        if (isTradeNoCell(cell) && !used[0])
        {
            return 0;
        }
        if (LONG_DIGITS.matcher(cell).matches() && !used[7])
        {
            return 7;
        }
        if (!used[6] && !isDateTimeCell(cell) && !isAmountCell(cell) && !isDirectionCell(cell))
        {
            return 6;
        }
        return -1;
    }

    private static int firstFree(boolean[] used)
    {
        for (int i = 0; i < used.length; i++)
        {
            if (!used[i])
            {
                return i;
            }
        }
        return -1;
    }

    private static boolean isDateTimeCell(String cell)
    {
        String normalized = cell.replace('年', '-').replace('月', '-').replace('日', ' ')
                .replace('/', '-').replace('.', '-').trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return DATE_TIME.matcher(normalized).matches()
                || DATE_TIME.matcher(cell.trim()).matches();
    }

    private static boolean isDirectionCell(String cell)
    {
        String v = cell.trim();
        return "支出".equals(v) || "收入".equals(v) || "其他".equals(v)
                || "收".equals(v) || "支".equals(v) || "收/支/其他".equals(v);
    }

    private static boolean isAmountCell(String cell)
    {
        String v = cell.trim().replace(",", "");
        if (LONG_DIGITS.matcher(v.replaceAll("[¥￥元]", "")).matches())
        {
            return false;
        }
        return AMOUNT_CELL.matcher(v).matches();
    }

    private static boolean isTradeTypeCell(String cell)
    {
        String v = cell.trim();
        return v.contains("商户消费") || v.contains("转账") || v.contains("红包")
                || v.contains("充值") || v.contains("提现") || v.contains("退款")
                || v.contains("扫二维码") || v.contains("二维码收款") || "消费".equals(v);
    }

    private static boolean isPaymentCell(String cell)
    {
        String v = cell.trim();
        return v.contains("银行") || v.contains("信用卡") || v.contains("储蓄卡")
                || v.contains("零钱") || v.contains("微信支付") || v.contains("花呗")
                || v.contains("借记卡")
                || (v.contains("卡") && v.matches(".*\\(\\d{4}\\).*"));
    }

    private static boolean isTradeNoCell(String cell)
    {
        String v = cell.trim();
        return LONG_DIGITS.matcher(v).matches() && v.length() >= 16;
    }

    private static BillVO fromTableCells(List<String> cells)
    {
        return fromTableCells(cells, true);
    }

    private static BillVO fromTableCells(List<String> cells, boolean strict)
    {
        List<String> aligned = cells.size() == 8 ? cells : padOrTrimTo8(cells);
        String tradeNo = aligned.get(0);
        String tradeTime = aligned.get(1);
        String tradeType = aligned.get(2);
        String direction = aligned.get(3);
        String paymentMethod = aligned.get(4);
        String amountStr = aligned.get(5);
        String merchant = aligned.get(6);
        String merchantOrderNo = aligned.get(7);

        if (strict)
        {
            String joined = String.join("|", aligned);
            // 整行明显是链接页脚，且没有金额特征才丢弃
            if (isJunkRowText(joined) && !HAS_AMOUNT_HINT.matcher(joined).find()
                    && parseAmountText(amountStr) == null)
            {
                return null;
            }
        }

        BigDecimal amount = parseAmountText(amountStr);
        // 金额列错位时，从本行其它单元格抢救金额（常见于列前推）
        if (amount == null)
        {
            amount = findAmountInCells(aligned);
        }
        if (amount == null && !StringUtils.hasText(merchant) && !StringUtils.hasText(tradeNo))
        {
            return null;
        }
        // 跳过明显非数据行 / 零金额页脚残留
        if ("金额".equals(amountStr) || "交易对方".equals(merchant))
        {
            return null;
        }
        if (amount != null && amount.signum() <= 0 && isJunkRowText(String.join("|", aligned)))
        {
            return null;
        }
        if (StringUtils.hasText(tradeNo) && isJunkRowText(tradeNo))
        {
            return null;
        }

        BillVO vo = new BillVO();
        vo.setTradeNo(blankToNull(tradeNo));
        vo.setTradeType(blankToNull(tradeType));
        // 收/支为空时默认支出；日期为空则保持空，禁止用其它行补齐
        vo.setDirection(StringUtils.hasText(direction) ? direction : "支出");
        vo.setPaymentMethod(blankToNull(paymentMethod));
        vo.setAmount(amount);
        vo.setMerchant(blankToNull(merchant));
        vo.setMerchantOrderNo(blankToNull(merchantOrderNo));
        vo.setNote(blankToNull(tradeType));
        applyTradeTime(vo, tradeTime);
        // 仅使用本行时间推导 billDate；解析失败则保持 null，不向前推
        if (vo.getBillDate() == null && StringUtils.hasText(tradeTime) && tradeTime.length() >= 10
                && isDateTimeCell(tradeTime))
        {
            try
            {
                String normalized = tradeTime.replace('/', '-').replace('.', '-');
                vo.setBillDate(LocalDate.parse(normalized.substring(0, 10)));
            }
            catch (Exception ignored)
            {
            }
        }
        // 无日期的行不要
        if (vo.getBillDate() == null && vo.getTradeTime() == null)
        {
            return null;
        }
        vo.setCategory(guessCategory(merchant, tradeType));
        vo.setAiConfidence(85);
        vo.setSource(1);
        vo.setSourceName("AI识别");
        return vo;
    }

    private static BigDecimal findAmountInCells(List<String> cells)
    {
        for (String cell : cells)
        {
            if (!StringUtils.hasText(cell) || isDateTimeCell(cell) || isTradeNoCell(cell)
                    || isDirectionCell(cell) || isTradeTypeCell(cell) || isPaymentCell(cell))
            {
                continue;
            }
            if (isAmountCell(cell))
            {
                BigDecimal amt = parseAmountText(cell);
                if (amt != null)
                {
                    return amt;
                }
            }
        }
        return null;
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
        // 无日期的行不要
        if (vo.getBillDate() == null && vo.getTradeTime() == null)
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