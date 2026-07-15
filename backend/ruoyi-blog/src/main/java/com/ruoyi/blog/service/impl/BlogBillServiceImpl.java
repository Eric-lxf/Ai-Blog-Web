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
            "请识别这张账单/收据图片，提取以下字段并以 JSON 格式返回：\n" +
            "billDate（消费日期，格式 yyyy-MM-dd）、\n" +
            "merchant（商户名称）、\n" +
            "category（消费类目，从以下选择：餐饮食品/购物消费/交通出行/水电燃气/医疗健康/健身娱乐/服饰购物/其他）、\n" +
            "amount（金额数字，不含货币符号）、\n" +
            "paymentMethod（支付方式）、\n" +
            "confidence（识别置信度 0-100 整数）。\n" +
            "只输出 JSON 对象，不要其他文字。";

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
    public BillVO recognize(BillRecognizeRequest request)
    {
        String raw = deepSeekService.recognizeImage(request.getImageUrl(), RECOGNIZE_PROMPT, AiModuleCode.BILL_VISION);
        return parseRecognizeResult(raw);
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

    private BillVO parseRecognizeResult(String raw)
    {
        BillVO vo = new BillVO();
        try
        {
            Matcher m = JSON_OBJECT.matcher(raw);
            if (!m.find()) { log.warn("AI 识别结果无法解析为 JSON：{}", raw); return vo; }
            JsonNode node = objectMapper.readTree(m.group());
            String dateStr = node.path("billDate").asText("");
            if (StringUtils.hasText(dateStr))
            {
                try { vo.setBillDate(LocalDate.parse(dateStr)); } catch (Exception ignored) {}
            }
            vo.setMerchant(node.path("merchant").asText(null));
            vo.setCategory(node.path("category").asText(null));
            String amtStr = node.path("amount").asText("");
            if (StringUtils.hasText(amtStr))
            {
                try { vo.setAmount(new BigDecimal(amtStr.replaceAll("[^\\d.]", ""))); } catch (Exception ignored) {}
            }
            vo.setPaymentMethod(node.path("paymentMethod").asText(null));
            int conf = node.path("confidence").asInt(0);
            vo.setAiConfidence(conf > 0 ? conf : null);
            vo.setSource(1);
            vo.setSourceName("AI识别");
        }
        catch (Exception e) { log.warn("解析 AI 识别 JSON 失败：{}", raw, e); }
        return vo;
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
        catch (Exception e) { log.warn("生成账单建议失败", e); return List.of(); }
    }
}