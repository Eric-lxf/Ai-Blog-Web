package com.ruoyi.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.blog.service.impl.BlogBillServiceImpl;
import com.ruoyi.blog.vo.BillVO;

class BlogBillRecognizeParseTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesMultiRowArrayFromWechatStyleStatement()
    {
        String raw = """
                [
                  {"billDate":"2026-07-14","merchant":"通行宝","category":"交通出行","amount":110.81,"paymentMethod":"招商银行信用卡(1683)","confidence":90},
                  {"billDate":"2026-07-13 18:20:11","merchant":"通行宝","category":"交通出行","amount":"28.29","paymentMethod":"招商银行信用卡(1683)","confidence":88},
                  {"billDate":"2026-07-11","merchant":"?","category":"其他","amount":400,"paymentMethod":"招商银行信用卡(1683)","note":"转账","confidence":70}
                ]
                """;
        List<BillVO> list = BlogBillServiceImpl.parseRecognizeResults(raw, mapper);
        assertEquals(3, list.size());
        assertEquals(new BigDecimal("110.81"), list.get(0).getAmount());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertEquals("2026-07-13", list.get(1).getBillDate().toString());
        assertEquals("转账", list.get(2).getNote());
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
    void dropsEmptyObjects()
    {
        String raw = "[{\"billDate\":\"\",\"merchant\":\"\",\"amount\":null},{\"billDate\":\"2026-07-14\",\"merchant\":\"通行宝\",\"amount\":9.5}]";
        List<BillVO> list = BlogBillServiceImpl.parseRecognizeResults(raw, mapper);
        assertEquals(1, list.size());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertNull(list.get(0).getCategory());
    }
}
