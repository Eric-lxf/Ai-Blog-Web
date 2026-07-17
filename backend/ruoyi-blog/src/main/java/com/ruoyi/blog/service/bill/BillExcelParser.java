package com.ruoyi.blog.service.bill;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;

import com.ruoyi.blog.vo.BillVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

/**
 * 解析微信/银行导出的 Excel 账单（.xls / .xlsx），按表头别名映射到 {@link BillVO}。
 */
public final class BillExcelParser
{
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.CHINA);

    private BillExcelParser()
    {
    }

    public static List<BillVO> parse(InputStream in)
    {
        try (Workbook workbook = WorkbookFactory.create(in))
        {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null)
            {
                throw new ServiceException("Excel 中没有工作表", HttpStatus.BAD_REQUEST);
            }
            int headerRowIdx = findHeaderRow(sheet);
            if (headerRowIdx < 0)
            {
                throw new ServiceException("未识别到账单表头（需包含金额、交易时间/日期等列）", HttpStatus.BAD_REQUEST);
            }
            Map<String, Integer> col = mapColumns(sheet.getRow(headerRowIdx));
            List<BillVO> list = new ArrayList<>();
            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++)
            {
                Row row = sheet.getRow(r);
                if (row == null)
                {
                    continue;
                }
                BillVO vo = toVo(row, col);
                if (vo != null)
                {
                    list.add(vo);
                }
            }
            return list;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("Excel 解析失败：" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private static int findHeaderRow(Sheet sheet)
    {
        int max = Math.min(sheet.getLastRowNum(), 30);
        for (int i = 0; i <= max; i++)
        {
            Row row = sheet.getRow(i);
            if (row == null)
            {
                continue;
            }
            Map<String, Integer> cols = mapColumns(row);
            boolean hasAmount = cols.containsKey("amount");
            boolean hasDate = cols.containsKey("tradeTime") || cols.containsKey("billDate");
            if (hasAmount && hasDate)
            {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, Integer> mapColumns(Row header)
    {
        Map<String, Integer> map = new HashMap<>();
        if (header == null)
        {
            return map;
        }
        short last = header.getLastCellNum();
        for (int c = 0; c < last; c++)
        {
            String name = normalizeHeader(cellText(header.getCell(c)));
            if (!StringUtils.hasText(name))
            {
                continue;
            }
            String key = matchField(name);
            if (key != null && !map.containsKey(key))
            {
                map.put(key, c);
            }
        }
        return map;
    }

    private static String matchField(String name)
    {
        if (containsAny(name, "交易单号", "订单号", "流水号", "微信订单号"))
        {
            return "tradeNo";
        }
        if (containsAny(name, "交易时间", "支付时间", "记账时间", "消费时间"))
        {
            return "tradeTime";
        }
        if (containsAny(name, "消费日期", "交易日期", "账单日期") || name.equals("日期"))
        {
            return "billDate";
        }
        if (containsAny(name, "交易类型", "业务类型") || name.equals("类型"))
        {
            return "tradeType";
        }
        if (containsAny(name, "收/支", "收支", "收/支/其他") || name.equals("方向"))
        {
            return "direction";
        }
        if (containsAny(name, "交易方式", "支付方式", "付款方式"))
        {
            return "paymentMethod";
        }
        if (containsAny(name, "金额(元)", "交易金额", "支出金额") || name.equals("金额") || name.endsWith("金额"))
        {
            return "amount";
        }
        if (containsAny(name, "交易对方", "商户名称", "对方户名", "商户") || name.equals("对方"))
        {
            return "merchant";
        }
        if (containsAny(name, "商户单号", "商家订单号"))
        {
            return "merchantOrderNo";
        }
        if (containsAny(name, "消费类目", "类目", "分类"))
        {
            return "category";
        }
        if (containsAny(name, "备注", "说明"))
        {
            return "note";
        }
        return null;
    }

    private static boolean containsAny(String name, String... keys)
    {
        for (String key : keys)
        {
            if (name.contains(key))
            {
                return true;
            }
        }
        return false;
    }

    private static String normalizeHeader(String raw)
    {
        if (!StringUtils.hasText(raw))
        {
            return "";
        }
        return raw.replace('\u00A0', ' ').trim().replaceAll("\\s+", "");
    }

    private static BillVO toVo(Row row, Map<String, Integer> col)
    {
        BigDecimal amount = parseAmount(cellText(row, col.get("amount")));
        String merchant = blankToNull(cellText(row, col.get("merchant")));
        String tradeNo = blankToNull(cellText(row, col.get("tradeNo")));
        if (amount == null && merchant == null && tradeNo == null)
        {
            return null;
        }

        BillVO vo = new BillVO();
        vo.setTradeNo(tradeNo);
        vo.setMerchant(merchant);
        vo.setAmount(amount);
        vo.setTradeType(blankToNull(cellText(row, col.get("tradeType"))));
        vo.setDirection(defaultDirection(cellText(row, col.get("direction"))));
        vo.setPaymentMethod(blankToNull(cellText(row, col.get("paymentMethod"))));
        vo.setMerchantOrderNo(blankToNull(cellText(row, col.get("merchantOrderNo"))));
        vo.setNote(blankToNull(cellText(row, col.get("note"))));
        String category = blankToNull(cellText(row, col.get("category")));
        vo.setCategory(category != null ? category : guessCategory(merchant, vo.getTradeType()));

        LocalDateTime tradeTime = parseDateTime(row, col.get("tradeTime"));
        if (tradeTime == null)
        {
            LocalDate billDate = parseDate(row, col.get("billDate"));
            if (billDate != null)
            {
                tradeTime = billDate.atStartOfDay();
            }
        }
        if (tradeTime != null)
        {
            vo.setTradeTime(tradeTime);
            vo.setBillDate(tradeTime.toLocalDate());
        }
        else
        {
            LocalDate billDate = parseDate(row, col.get("billDate"));
            vo.setBillDate(billDate);
        }
        // 无日期的行不要
        if (vo.getBillDate() == null)
        {
            return null;
        }
        if (amount == null && merchant == null && tradeNo == null)
        {
            return null;
        }
        vo.setSource(1);
        vo.setSourceName("AI识别");
        vo.setAiConfidence(100);
        return vo;
    }

    private static String defaultDirection(String raw)
    {
        if (!StringUtils.hasText(raw))
        {
            return "支出";
        }
        String v = raw.trim();
        if (v.contains("收入") || v.contains("入账") || "收".equals(v))
        {
            return "收入";
        }
        if (v.contains("支出") || "支".equals(v))
        {
            return "支出";
        }
        return v;
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

    private static LocalDateTime parseDateTime(Row row, Integer idx)
    {
        if (idx == null)
        {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null)
        {
            return null;
        }
        try
        {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
            {
                Date date = cell.getDateCellValue();
                return LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("Asia/Shanghai"));
            }
        }
        catch (Exception ignored)
        {
        }
        String text = cellText(cell);
        if (!StringUtils.hasText(text))
        {
            return null;
        }
        String normalized = text.replace('T', ' ').trim();
        try
        {
            if (normalized.length() >= 19)
            {
                return LocalDateTime.parse(normalized.substring(0, 19), DT);
            }
            if (normalized.length() >= 16)
            {
                return LocalDateTime.parse(normalized.substring(0, 16), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            if (normalized.length() >= 10)
            {
                return LocalDate.parse(normalized.substring(0, 10)).atStartOfDay();
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private static LocalDate parseDate(Row row, Integer idx)
    {
        LocalDateTime dt = parseDateTime(row, idx);
        return dt == null ? null : dt.toLocalDate();
    }

    private static BigDecimal parseAmount(String text)
    {
        if (!StringUtils.hasText(text))
        {
            return null;
        }
        try
        {
            String cleaned = text.replace(",", "").replace("，", "").replaceAll("[^\\d.\\-]", "");
            if (!StringUtils.hasText(cleaned) || "-".equals(cleaned))
            {
                return null;
            }
            return new BigDecimal(cleaned).abs();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String cellText(Row row, Integer idx)
    {
        if (idx == null || row == null)
        {
            return "";
        }
        return cellText(row.getCell(idx));
    }

    private static String cellText(Cell cell)
    {
        if (cell == null)
        {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static String blankToNull(String v)
    {
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}
