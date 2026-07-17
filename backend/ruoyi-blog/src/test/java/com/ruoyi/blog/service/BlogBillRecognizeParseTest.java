package com.ruoyi.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.blog.service.impl.BlogBillServiceImpl;
import com.ruoyi.blog.service.llm.LlmClientImpl;
import com.ruoyi.blog.vo.BillVO;

class BlogBillRecognizeParseTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesWechatMarkdownTableAllRows()
    {
        String raw = """
                |交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|
                |---|---|---|---|---|---|---|---|
                |4200003099202607145869|2026-07-14 09:31:21|商户消费|支出|招商银行信用卡(1683)|110.81|通行宝|10001|
                |4200003099202607130001|2026-07-13 18:20:11|商户消费|支出|招商银行信用卡(1683)|28.29|通行宝|10002|
                |4200003099202607110002|2026-07-11 12:00:00|转账|支出|建设银行储蓄卡(0597)|400.00|张三|10003|
                |4200003099202607110003|2026-07-11 10:00:00|商户消费|支出|招商银行信用卡(1683)|9.50|通行宝|10004|
                """;
        List<BillVO> list = BlogBillServiceImpl.parseMarkdownTable(raw);
        assertEquals(4, list.size());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertEquals("招商银行信用卡(1683)", list.get(0).getPaymentMethod());
        assertEquals(new BigDecimal("110.81"), list.get(0).getAmount());
        assertEquals("交通出行", list.get(0).getCategory());
        assertEquals("2026-07-14T09:31:21", list.get(0).getTradeTime().toString());
        assertEquals("转账", list.get(2).getTradeType());
        assertEquals("其他", list.get(2).getCategory());
    }

    @Test
    void parsesMultiRowArrayFromWechatStyleStatement()
    {
        String raw = """
                [
                  {"tradeNo":"42000001","tradeTime":"2026-07-14 09:31:21","billDate":"2026-07-14","tradeType":"商户消费","direction":"支出","merchant":"通行宝","category":"交通出行","amount":110.81,"paymentMethod":"招商银行信用卡(1683)","merchantOrderNo":"10001","confidence":90},
                  {"billDate":"2026-07-13 18:20:11","merchant":"通行宝","category":"交通出行","amount":"28.29","paymentMethod":"招商银行信用卡(1683)","confidence":88},
                  {"billDate":"2026-07-11","merchant":"?","tradeType":"转账","amount":400,"paymentMethod":"招商银行信用卡(1683)","confidence":70}
                ]
                """;
        List<BillVO> list = BlogBillServiceImpl.parseRecognizeResults(raw, mapper);
        assertEquals(3, list.size());
        assertEquals(new BigDecimal("110.81"), list.get(0).getAmount());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertEquals("42000001", list.get(0).getTradeNo());
        assertEquals("2026-07-13", list.get(1).getBillDate().toString());
        assertEquals("转账", list.get(2).getTradeType());
        assertEquals("其他", list.get(2).getCategory());
    }

    @Test
    void recoversMultipleObjectsFromTruncatedArray()
    {
        // 模拟 max_tokens 截断：缺少结尾 ]
        String raw = "[{\"billDate\":\"2026-07-14\",\"merchant\":\"通行宝\",\"amount\":110.81},{\"billDate\":\"2026-07-13\",\"merchant\":\"通行宝\",\"amount\":28.29},{\"billDate\":\"2026-07-11\",\"merchant\":\"通行宝\",\"amount\":9.5";
        List<BillVO> list = BlogBillServiceImpl.parseRecognizeResults(raw, mapper);
        assertEquals(2, list.size());
        assertEquals(new BigDecimal("110.81"), list.get(0).getAmount());
        assertEquals(new BigDecimal("28.29"), list.get(1).getAmount());
    }

    @Test
    void fallsBackToSingleObjectForReceipt()
    {
        String raw = "识别结果：{\"billDate\":\"2026-07-14\",\"merchant\":\"咖啡店\",\"category\":\"餐饮食品\",\"amount\":32.5,\"paymentMethod\":\"微信支付\",\"confidence\":95}";
        List<BillVO> list = BlogBillServiceImpl.parseRecognizeResults(raw, mapper);
        assertEquals(1, list.size());
        assertEquals("咖啡店", list.get(0).getMerchant());
        assertEquals(new BigDecimal("32.5"), list.get(0).getAmount());
    }

    @Test
    void dropsEmptyObjectsAndGuessesCategory()
    {
        String raw = "[{\"billDate\":\"\",\"merchant\":\"\",\"amount\":null},{\"billDate\":\"2026-07-14\",\"merchant\":\"通行宝\",\"amount\":9.5}]";
        List<BillVO> list = BlogBillServiceImpl.parseRecognizeResults(raw, mapper);
        assertEquals(1, list.size());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertEquals("交通出行", list.get(0).getCategory());
    }

    @Test
    void extractsOpenAiContentFromTextOrPartsArray()
    {
        String asText = "{\"choices\":[{\"message\":{\"content\":\"[{\\\"amount\\\":1}]\"}}]}";
        String asParts = "{\"choices\":[{\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"[{\\\"amount\\\":2}]\"}]}}]}";
        try
        {
            assertTrue(LlmClientImpl.extractOpenAiMessageContent(mapper.readTree(asText)).contains("amount"));
            assertTrue(LlmClientImpl.extractOpenAiMessageContent(mapper.readTree(asParts)).contains("2"));
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
    }

    @Test
    void dropsRowsWithoutDate()
    {
        // 无日期的行不要；商户单号为空用 || 占位且不前移
        String raw = """
                |交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|
                |---|---|---|---|---|---|---|---|
                |4200003099202607145869||商户消费|支出|招商银行信用卡(1683)|110.81|通行宝|10001|
                |4200003099202607145870|2026-07-14 09:31:21|商户消费|支出|零钱|9.90|咖啡店||
                """;
        List<BillVO> list = BlogBillServiceImpl.parseMarkdownTable(raw);
        assertEquals(1, list.size());
        assertEquals("咖啡店", list.get(0).getMerchant());
        assertEquals(new BigDecimal("9.90"), list.get(0).getAmount());
        assertEquals(null, list.get(0).getMerchantOrderNo());
    }

    @Test
    void realignsWhenOcrOmitsEmptyColumnPipes()
    {
        // OCR 漏掉空的商户单号列，日期仍在，应重对齐并保留
        String raw = """
                |4200003099202607145869|2026-07-14 09:31:21|商户消费|支出|招商银行信用卡(1683)|110.81|通行宝|
                """;
        List<BillVO> list = BlogBillServiceImpl.parseMarkdownTable(raw);
        assertEquals(1, list.size());
        assertEquals("2026-07-14T09:31:21", list.get(0).getTradeTime().toString());
        assertEquals("商户消费", list.get(0).getTradeType());
        assertEquals(new BigDecimal("110.81"), list.get(0).getAmount());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertEquals("招商银行信用卡(1683)", list.get(0).getPaymentMethod());
    }

    @Test
    void dropsFooterLinkRows()
    {
        String raw = """
                |交易单号|交易时间|交易类型|收/支/其他|交易方式|金额|交易对方|商户单号|
                |---|---|---|---|---|---|---|---|
                |4200003099202607145869|2026-07-14 09:31:21|商户消费|支出|零钱|9.90|咖啡店|10001|
                |https://pay.weixin.qq.com/index.php/public/wechatpay|查看详情|客服|帮助中心|加载更多|0|点击这里|页脚|
                |第 2 页||||链接||||
                """;
        List<BillVO> list = BlogBillServiceImpl.parseMarkdownTable(raw);
        assertEquals(1, list.size());
        assertEquals("咖啡店", list.get(0).getMerchant());
    }

    @Test
    void parsesFullwidthPipeTables()
    {
        // qwen OCR 常输出全角竖线，旧逻辑会整表跳过导致「识别不到」
        String raw = """
                ｜交易单号｜交易时间｜交易类型｜收/支/其他｜交易方式｜金额｜交易对方｜商户单号｜
                ｜---｜---｜---｜---｜---｜---｜---｜---｜
                ｜4200003099202607145869｜2026-07-14 09:31:21｜商户消费｜支出｜零钱｜9.90｜美团客服中心｜10001｜
                """;
        List<BillVO> list = BlogBillServiceImpl.parseMarkdownTable(raw);
        assertEquals(1, list.size());
        assertEquals(new BigDecimal("9.90"), list.get(0).getAmount());
        assertEquals("美团客服中心", list.get(0).getMerchant());
        assertEquals("10001", list.get(0).getMerchantOrderNo());
    }

    @Test
    void qualityCheckPassesCompleteRows()
    {
        List<BillVO> list = BlogBillServiceImpl.parseMarkdownTable("""
                |4200003099202607145869|2026-07-14 09:31:21|商户消费|支出|零钱|9.90|咖啡店|10001|
                """);
        assertEquals(null, BlogBillServiceImpl.findRecognizeQualityIssue(list));
    }

    @Test
    void qualityCheckFailsWhenAmountMissingOrMisaligned()
    {
        BillVO bad = new BillVO();
        bad.setBillDate(java.time.LocalDate.of(2026, 7, 14));
        bad.setMerchant("咖啡店");
        bad.setAmount(null);
        assertTrue(BlogBillServiceImpl.findRecognizeQualityIssue(java.util.List.of(bad)).contains("金额"));

        BillVO swapped = new BillVO();
        swapped.setBillDate(java.time.LocalDate.of(2026, 7, 14));
        swapped.setAmount(new BigDecimal("9.90"));
        swapped.setMerchant("9.90");
        swapped.setTradeNo("4200003099202607145869");
        assertTrue(BlogBillServiceImpl.findRecognizeQualityIssue(java.util.List.of(swapped)).contains("错位"));
    }
}
