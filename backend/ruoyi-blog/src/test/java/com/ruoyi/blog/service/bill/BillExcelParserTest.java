package com.ruoyi.blog.service.bill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.ruoyi.blog.vo.BillVO;
import com.ruoyi.common.exception.ServiceException;

class BillExcelParserTest
{
    @Test
    void parsesWechatStyleExportWithPreambleRows() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet sheet = wb.createSheet("明细");
            sheet.createRow(0).createCell(0).setCellValue("微信支付账单明细");
            sheet.createRow(1).createCell(0).setCellValue("导出时间：2026-07-16");
            Row header = sheet.createRow(3);
            header.createCell(0).setCellValue("交易时间");
            header.createCell(1).setCellValue("交易类型");
            header.createCell(2).setCellValue("交易对方");
            header.createCell(3).setCellValue("收/支");
            header.createCell(4).setCellValue("金额(元)");
            header.createCell(5).setCellValue("交易方式");
            header.createCell(6).setCellValue("交易单号");
            header.createCell(7).setCellValue("商户单号");

            Row r1 = sheet.createRow(4);
            r1.createCell(0).setCellValue("2026-07-14 09:31:21");
            r1.createCell(1).setCellValue("商户消费");
            r1.createCell(2).setCellValue("通行宝");
            r1.createCell(3).setCellValue("支出");
            r1.createCell(4).setCellValue("110.81");
            r1.createCell(5).setCellValue("招商银行信用卡(1683)");
            r1.createCell(6).setCellValue("4200003099202607145869");
            r1.createCell(7).setCellValue("10001");

            Row r2 = sheet.createRow(5);
            r2.createCell(0).setCellValue("2026-07-13 18:20:11");
            r2.createCell(1).setCellValue("商户消费");
            r2.createCell(2).setCellValue("通行宝");
            r2.createCell(3).setCellValue("支出");
            r2.createCell(4).setCellValue(28.29);
            r2.createCell(5).setCellValue("招商银行信用卡(1683)");
            r2.createCell(6).setCellValue("4200003099202607130001");
            r2.createCell(7).setCellValue("10002");

            wb.write(out);
            bytes = out.toByteArray();
        }

        List<BillVO> list = BillExcelParser.parse(new ByteArrayInputStream(bytes));
        assertEquals(2, list.size());
        assertEquals(new BigDecimal("110.81"), list.get(0).getAmount());
        assertEquals("通行宝", list.get(0).getMerchant());
        assertEquals("支出", list.get(0).getDirection());
        assertEquals("招商银行信用卡(1683)", list.get(0).getPaymentMethod());
        assertEquals("4200003099202607145869", list.get(0).getTradeNo());
        assertEquals("交通出行", list.get(0).getCategory());
        assertEquals("2026-07-14", list.get(0).getBillDate().toString());
        assertEquals(new BigDecimal("28.29"), list.get(1).getAmount());
    }

    @Test
    void rejectsWorkbookWithoutBillHeader() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("年龄");
            sheet.createRow(1).createCell(0).setCellValue("张三");
            wb.write(out);
            bytes = out.toByteArray();
        }
        ServiceException ex = assertThrows(ServiceException.class,
                () -> BillExcelParser.parse(new ByteArrayInputStream(bytes)));
        assertTrue(ex.getMessage().contains("表头"));
    }
}
