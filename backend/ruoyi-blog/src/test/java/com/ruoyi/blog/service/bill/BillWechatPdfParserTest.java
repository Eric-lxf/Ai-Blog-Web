package com.ruoyi.blog.service.bill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ruoyi.blog.vo.BillVO;

/**
 * 对照真实《微信支付交易明细证明》样例：5 页、97 笔，每笔均有交易时间。
 */
class BillWechatPdfParserTest
{
    @Test
    void parseSampleWechatDetailPdf_keepsAllRowsAndTimes() throws Exception
    {
        try (InputStream in = getClass().getResourceAsStream("/bill/wechat-detail-sample.pdf"))
        {
            assertNotNull(in, "缺少测试资源 bill/wechat-detail-sample.pdf");
            List<BillVO> rows = BillWechatPdfParser.parse(in);
            assertTrue(BillWechatPdfParser.looksComplete(rows), "本地抽表应判定完整");
            assertEquals(97, rows.size(), "样例 PDF 应解析出 97 笔明细");

            long withTime = rows.stream().filter(r -> r.getTradeTime() != null).count();
            assertEquals(97, withTime, "每笔都应有完整交易时间");

            BillVO first = rows.get(0);
            assertNotNull(first.getTradeNo());
            assertTrue(first.getTradeNo().length() >= 18);
            assertNotNull(first.getAmount());
            assertNotNull(first.getBillDate());

            // 首页首笔（样例）：2026-07-14 09:31:21 / 通行宝 / 商户单号完整
            assertEquals(2026, first.getTradeTime().getYear());
            assertEquals(7, first.getTradeTime().getMonthValue());
            assertEquals(14, first.getTradeTime().getDayOfMonth());
            assertEquals(9, first.getTradeTime().getHour());
            assertEquals(31, first.getTradeTime().getMinute());
            assertEquals("4200003099202607149618335869", first.getTradeNo());
            assertEquals("通行宝", first.getMerchant());
            assertEquals("10122607140300060762", first.getMerchantOrderNo());
            assertEquals(0, new java.math.BigDecimal("110.81").compareTo(first.getAmount()));
        }
    }
}
