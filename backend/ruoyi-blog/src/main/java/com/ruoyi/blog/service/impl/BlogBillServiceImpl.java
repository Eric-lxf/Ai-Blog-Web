package com.ruoyi.blog.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    private static final String RECOGNIZE_PROMPT =
            "请识别这张账单/收据/交易明细图片。若图片中有多笔交易（如微信支付交易明细、银行流水表），必须逐行全部提取，不要只返回第一笔。\n" +
            "仅提取支出类记录；忽略收入、退款入账（若无法判断则保留并标注）。\n" +
            "以 JSON 数组返回，每个元素包含：\n" +
            "billDate（消费日期，格式 yyyy-MM-dd，取交易时间的日期部分）、\n" +
            "merchant（商户/交易对方名称）、\n" +
            "category（消费类目，从以下选择：餐饮食品/购物消费/交通出行/水电燃气/医疗健康/健身娱乐/服饰购物/其他；通行宝/ETC/地铁/打车等归交通出行，转账归其他）、\n" +
            "amount（金额数字，不含货币符号）、\n" +
            "paymentMethod（支付方式，如银行卡名+尾号、零钱等）、\n" +
            "note（可选，交易类型如商户消费/转账）、\n" +
            "confidence（识别置信度 0-100 整数）。\n" +
            "只输出 JSON 数组；若仅一笔也用长度为 1 的数组。不要输出其他文字。";

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
        if (query.getStartDate() != null)
        {
            wrapper.ge(BlogBill::getBillDate, query.getStartDate());
        }
        if (query.getEndDate() != null)
        {
            wrapper.le(BlogBill::getBillDate, query.getEndDate());
        }
        wrapper.orderByDesc(BlogBill::getBillDate, BlogBill::getCreateTime);
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
        bill.setMerchant(request.getMerchant());
        bill.setCategory(request.getCategory());
        bill.setAmount(request.getAmount());
        bill.setPaymentMethod(request.getPaymentMethod());
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
        String imageUrl = validateRecognizeImageUrl(request.getImageUrl());
        String raw = deepSeekService.recognizeImage(imageUrl, RECOGNIZE_PROMPT, AiModuleCode.BILL_VISION);
        List<BillVO> list = parseRecognizeResults(raw, objectMapper);
        if (list.isEmpty())
        {
            throw new ServiceException("未能从图片中识别到账单明细，请换更清晰的原图后重试", HttpStatus.ERROR);
        }
        return list;
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
                .le(BlogBill::getBillDate, endDate));

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
     * 解析 AI 返回：优先 JSON 数组（多行明细），兼容单对象。
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
            Matcher arrayMatcher = JSON_ARRAY.matcher(raw);
            if (arrayMatcher.find())
            {
                JsonNode arr = mapper.readTree(arrayMatcher.group());
                if (arr.isArray())
                {
                    for (JsonNode node : arr)
                    {
                        BillVO vo = toRecognizeVo(node);
                        if (vo != null)
                        {
                            list.add(vo);
                        }
                    }
                    return list;
                }
            }
            Matcher objectMatcher = JSON_OBJECT.matcher(raw);
            if (objectMatcher.find())
            {
                BillVO vo = toRecognizeVo(mapper.readTree(objectMatcher.group()));
                if (vo != null)
                {
                    list.add(vo);
                }
            }
            else
            {
                log.warn("AI 识别结果无法解析为 JSON：{}", raw);
            }
        }
        catch (Exception e)
        {
            log.warn("解析 AI 识别 JSON 失败：{}", raw, e);
        }
        return list;
    }

    private static BillVO toRecognizeVo(JsonNode node)
    {
        if (node == null || !node.isObject())
        {
            return null;
        }
        BillVO vo = new BillVO();
        String dateStr = node.path("billDate").asText("");
        if (StringUtils.hasText(dateStr))
        {
            String normalized = dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr;
            try
            {
                vo.setBillDate(LocalDate.parse(normalized));
            }
            catch (Exception ignored)
            {
                // leave null for用户前端补全
            }
        }
        String merchant = textOrNull(node, "merchant");
        vo.setMerchant(merchant);
        vo.setCategory(textOrNull(node, "category"));
        BigDecimal amount = parseAmount(node.get("amount"));
        vo.setAmount(amount);
        vo.setPaymentMethod(textOrNull(node, "paymentMethod"));
        vo.setNote(textOrNull(node, "note"));
        int conf = node.path("confidence").asInt(0);
        vo.setAiConfidence(conf > 0 ? conf : null);
        vo.setSource(1);
        vo.setSourceName("AI识别");
        // 无金额且无商户的空对象丢弃
        if (amount == null && !StringUtils.hasText(merchant))
        {
            return null;
        }
        return vo;
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