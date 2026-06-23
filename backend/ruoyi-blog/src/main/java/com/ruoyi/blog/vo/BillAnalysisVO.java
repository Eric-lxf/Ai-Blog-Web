package com.ruoyi.blog.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BillAnalysisVO
{

    private SummaryData summary;
    private List<BillCategoryAmountVO> categoryPie;
    private List<String> months;
    private List<MonthlySeriesItem> monthlyTrend;
    private List<AdviceItem> aiAdvice;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryData
    {
        private BigDecimal totalAmount;
        private BigDecimal monthlyAvg;
        private long billCount;
        private String topCategory;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySeriesItem
    {
        private String name;
        private List<BigDecimal> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdviceItem
    {
        /** warning / info / success / danger */
        private String tone;
        private String title;
        private String detail;
    }
}
