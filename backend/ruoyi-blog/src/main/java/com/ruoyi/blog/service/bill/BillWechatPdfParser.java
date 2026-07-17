package com.ruoyi.blog.service.bill;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ruoyi.blog.vo.BillVO;

/**
 * 微信「交易明细证明」类 PDF 本地抽表。
 * <p>
 * 真实样例特征：
 * <ul>
 *   <li>仅第 1 页有表头：交易单号|交易时间|交易类型|收/支/其他|交易方式|金额(元)|交易对方|商户单号</li>
 *   <li>第 2 页起无表头，表体直接续排</li>
 *   <li>单元格多行折行（单号/时间/银行卡名）</li>
 *   <li>末页「说明：」起为法律声明，非数据</li>
 * </ul>
 * 原生文字 PDF 应走本解析，避免视觉 OCR 丢时间、丢行。
 */
public final class BillWechatPdfParser
{
    private static final Logger log = LoggerFactory.getLogger(BillWechatPdfParser.class);

    private static final Pattern TRADE_NO = Pattern.compile("^[0-9A-Za-z]{18,}$");
    /**
     * PDFBox 常把「交易单号+日期」粘成一段，例如
     * {@code 4200003099202607149618332026-07-14}
     */
    private static final Pattern TRADE_NO_GLUED_DATE = Pattern.compile(
            "^([0-9A-Za-z]{18,})(20\\d{2}-\\d{2}-\\d{2})$");
    private static final Pattern DATE_TIME = Pattern.compile(
            "(20\\d{2})[-/年.](\\d{1,2})[-/月.](\\d{1,2})日?\\s*(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern DATE_ONLY = Pattern.compile(
            "(20\\d{2})[-/年.](\\d{1,2})[-/月.](\\d{1,2})日?");
    private static final Pattern AMOUNT = Pattern.compile("([+-]?\\d+(?:\\.\\d{1,2})?)");

    /**
     * 列左边界（来自真实样例数据列 x0，页宽约 595pt）：
     * 交易单号 / 交易时间 / 交易类型 / 收支 / 交易方式 / 金额 / 交易对方 / 商户单号
     */
    private static final float[] DEFAULT_COL_LEFT = {
            0f, 125f, 195f, 245f, 305f, 355f, 405f, 455f
    };

    /** 兼容旧逻辑：拆粘连日期时落到时间列中部 */
    private static final float TIME_COL_X = 160f;

    private BillWechatPdfParser()
    {
    }

    public static List<BillVO> parse(InputStream in) throws IOException
    {
        byte[] bytes = in.readAllBytes();
        return parseBytes(bytes).items;
    }

    public static List<BillVO> parse(byte[] pdfBytes)
    {
        return parseBytes(pdfBytes).items;
    }

    /**
     * 本地抽表是否足够完整：有数据且绝大多数行带交易时间/日期。
     */
    public static boolean looksComplete(List<BillVO> rows)
    {
        if (rows == null || rows.isEmpty())
        {
            return false;
        }
        int withTime = 0;
        for (BillVO vo : rows)
        {
            if (vo.getTradeTime() != null || vo.getBillDate() != null)
            {
                withTime++;
            }
        }
        if (rows.size() >= 3 && withTime * 100 / rows.size() < 80)
        {
            return false;
        }
        return withTime > 0;
    }

    private static final class InternalResult
    {
        final List<BillVO> items;

        InternalResult(List<BillVO> items)
        {
            this.items = items != null ? items : new ArrayList<>();
        }
    }

    private static InternalResult parseBytes(byte[] pdfBytes)
    {
        if (pdfBytes == null || pdfBytes.length == 0)
        {
            return new InternalResult(new ArrayList<>());
        }
        List<BillVO> all = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes)))
        {
            int pages = doc.getNumberOfPages();
            float[] colLeft = DEFAULT_COL_LEFT;
            for (int p = 1; p <= pages; p++)
            {
                List<Line> lines = extractLines(doc, p);
                all.addAll(parsePageLines(lines, colLeft));
            }
        }
        catch (Exception e)
        {
            log.warn("微信明细 PDF 本地解析失败: {}", e.getMessage());
            return new InternalResult(new ArrayList<>());
        }
        int withTime = 0;
        for (BillVO vo : all)
        {
            if (vo.getTradeTime() != null || vo.getBillDate() != null)
            {
                withTime++;
            }
        }
        log.info("微信明细 PDF 本地解析: rows={}, withTime={}", all.size(), withTime);
        return new InternalResult(all);
    }

    private static List<Line> extractLines(PDDocument doc, int pageIndex) throws IOException
    {
        List<Word> words = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper()
        {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions)
            {
                if (textPositions == null || textPositions.isEmpty())
                {
                    return;
                }
                StringBuilder buf = new StringBuilder();
                float x0 = textPositions.get(0).getXDirAdj();
                float y0 = textPositions.get(0).getYDirAdj();
                float x1 = x0;
                for (TextPosition tp : textPositions)
                {
                    buf.append(tp.getUnicode());
                    x1 = Math.max(x1, tp.getXDirAdj() + tp.getWidthDirAdj());
                    y0 = tp.getYDirAdj();
                }
                String t = buf.toString().trim();
                if (t.isEmpty())
                {
                    return;
                }
                // PDFBox 常把单号与日期粘在同一 run：拆开并落到时间列
                Matcher glued = TRADE_NO_GLUED_DATE.matcher(t.replace(" ", ""));
                if (glued.matches())
                {
                    String no = glued.group(1);
                    String date = glued.group(2);
                    words.add(new Word(no, x0, Math.min(x1, x0 + 90f), y0));
                    words.add(new Word(date, TIME_COL_X - 10f, TIME_COL_X + 40f, y0));
                    return;
                }
                words.add(new Word(t, x0, x1, y0));
            }
        };
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        stripper.setSortByPosition(true);
        stripper.getText(doc);

        // getYDirAdj 在本类 PDF 中随行向下增大，按 Y 升序即从上到下
        words.sort(Comparator.comparingDouble((Word w) -> w.y).thenComparingDouble(w -> w.x0));
        List<Line> lines = new ArrayList<>();
        final float yTol = 3.5f;
        for (Word w : words)
        {
            if (lines.isEmpty() || Math.abs(lines.get(lines.size() - 1).y - w.y) > yTol)
            {
                lines.add(new Line(w.y));
            }
            lines.get(lines.size() - 1).words.add(w);
        }
        for (Line line : lines)
        {
            line.words.sort(Comparator.comparingDouble(w -> w.x0));
            StringBuilder sb = new StringBuilder();
            for (Word w : line.words)
            {
                if (sb.length() > 0)
                {
                    sb.append(' ');
                }
                sb.append(w.text);
            }
            line.text = sb.toString().replaceAll("\\s+", " ").trim();
        }
        return lines;
    }

    private static List<BillVO> parsePageLines(List<Line> lines, float[] colLeft)
    {
        List<String[]> rows = new ArrayList<>();
        String[] current = null;
        boolean inTable = false;

        for (Line line : lines)
        {
            String t = line.text;
            if (!StringUtils.hasText(t))
            {
                continue;
            }
            if (t.startsWith("说明") || t.contains("本证明仅用于查看"))
            {
                break;
            }
            String compact = t.replace(" ", "");
            if (compact.contains("交易单号") && compact.contains("交易时间"))
            {
                inTable = true;
                continue;
            }
            if (compact.contains("微信支付交易明细证明") || compact.startsWith("证明编号")
                    || compact.startsWith("昵称") || compact.startsWith("微信号")
                    || compact.startsWith("起始时间") || compact.startsWith("申请时间")
                    || compact.contains("交易明细对应时间段") || compact.contains("具体交易明细"))
            {
                continue;
            }

            String[] cells = assignToColumns(line, colLeft);
            unglueTradeAndDate(cells);
            boolean newRecord = looksLikeNewTradeRow(cells);

            if (!inTable && newRecord)
            {
                inTable = true;
            }
            if (!inTable)
            {
                continue;
            }

            if (newRecord)
            {
                if (current != null)
                {
                    rows.add(current);
                }
                current = new String[8];
                for (int i = 0; i < 8; i++)
                {
                    current[i] = emptyToNull(cells[i]);
                }
            }
            else if (current != null)
            {
                for (int i = 0; i < 8; i++)
                {
                    if (StringUtils.hasText(cells[i]))
                    {
                        current[i] = appendCell(current[i], cells[i]);
                    }
                }
            }
        }
        if (current != null)
        {
            rows.add(current);
        }

        List<BillVO> items = new ArrayList<>();
        for (String[] row : rows)
        {
            BillVO item = toVo(row);
            if (item != null)
            {
                items.add(item);
            }
        }
        return items;
    }

    private static String[] assignToColumns(Line line, float[] colLeft)
    {
        String[] cells = new String[8];
        for (Word w : line.words)
        {
            float mid = (w.x0 + w.x1) / 2f;
            int best = 0;
            for (int i = 0; i < colLeft.length; i++)
            {
                if (mid >= colLeft[i])
                {
                    best = i;
                }
            }
            cells[best] = appendCell(cells[best], w.text);
        }
        return cells;
    }

    /** 把粘在交易单号末尾的 {@code yyyy-MM-dd} 拆到交易时间列。 */
    private static void unglueTradeAndDate(String[] cells)
    {
        if (!StringUtils.hasText(cells[0]))
        {
            return;
        }
        Matcher m = TRADE_NO_GLUED_DATE.matcher(cells[0].replace(" ", ""));
        if (!m.matches())
        {
            return;
        }
        cells[0] = m.group(1);
        String date = m.group(2);
        cells[1] = StringUtils.hasText(cells[1]) ? date + cells[1].replace(" ", "") : date;
    }

    private static boolean looksLikeNewTradeRow(String[] cells)
    {
        if (!StringUtils.hasText(cells[0]))
        {
            return false;
        }
        String c0 = cells[0].replace(" ", "");
        if (TRADE_NO.matcher(c0).matches())
        {
            // 续行尾巴（如 4 位单号后缀）不应开新行：新行通常还带类型/金额/时间
            boolean hasOther = StringUtils.hasText(cells[1]) || StringUtils.hasText(cells[2])
                    || StringUtils.hasText(cells[5]) || StringUtils.hasText(cells[6]);
            return hasOther || c0.length() >= 22;
        }
        return TRADE_NO_GLUED_DATE.matcher(c0).matches();
    }

    private static BillVO toVo(String[] row)
    {
        String tradeNo = clean(row[0]);
        String timeRaw = clean(row[1]);
        String tradeType = clean(row[2]);
        String direction = clean(row[3]);
        String payMethod = clean(row[4]);
        String amountRaw = clean(row[5]);
        String merchant = clean(row[6]);
        String merchantOrderNo = clean(row[7]);

        if (!StringUtils.hasText(tradeNo) && !StringUtils.hasText(amountRaw) && !StringUtils.hasText(merchant))
        {
            return null;
        }
        if (StringUtils.hasText(tradeNo))
        {
            tradeNo = tradeNo.replace(" ", "");
        }
        if (StringUtils.hasText(merchantOrderNo))
        {
            merchantOrderNo = merchantOrderNo.replace(" ", "");
        }

        LocalDateTime tradeTime = parseDateTime(timeRaw);
        LocalDate billDate = tradeTime != null ? tradeTime.toLocalDate() : parseDateOnly(timeRaw);
        if (tradeTime == null && billDate == null)
        {
            return null;
        }

        BillVO vo = new BillVO();
        vo.setTradeNo(tradeNo);
        vo.setTradeTime(tradeTime);
        vo.setBillDate(billDate);
        vo.setTradeType(tradeType);
        vo.setDirection(normalizeDirection(direction));
        vo.setPaymentMethod(payMethod);
        vo.setAmount(parseAmount(amountRaw));
        vo.setMerchant(merchant);
        vo.setMerchantOrderNo(merchantOrderNo);
        vo.setCategory(guessCategory(merchant, tradeType));
        vo.setSource(1);
        vo.setSourceName("AI识别");
        vo.setAiConfidence(100);
        return vo;
    }

    private static LocalDateTime parseDateTime(String raw)
    {
        if (!StringUtils.hasText(raw))
        {
            return null;
        }
        String s = raw.replace(" ", "");
        Matcher m = DATE_TIME.matcher(s);
        if (m.find())
        {
            int sec = m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;
            return LocalDateTime.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4)),
                    Integer.parseInt(m.group(5)),
                    sec);
        }
        return null;
    }

    private static LocalDate parseDateOnly(String raw)
    {
        if (!StringUtils.hasText(raw))
        {
            return null;
        }
        Matcher m = DATE_ONLY.matcher(raw.replace(" ", ""));
        if (m.find())
        {
            return LocalDate.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3)));
        }
        return null;
    }

    private static String normalizeDirection(String raw)
    {
        if (!StringUtils.hasText(raw))
        {
            return "支出";
        }
        if (raw.contains("收入"))
        {
            return "收入";
        }
        if (raw.contains("支出"))
        {
            return "支出";
        }
        if (raw.contains("其他") || "/".equals(raw.trim()))
        {
            return "其他";
        }
        return raw.trim();
    }

    private static String guessCategory(String merchant, String tradeType)
    {
        String m = merchant == null ? "" : merchant;
        String t = tradeType == null ? "" : tradeType;
        if (m.contains("通行宝") || m.contains("地铁") || m.contains("滴滴") || m.contains("高德") || m.contains("ETC"))
        {
            return "交通出行";
        }
        if (t.contains("转账") || t.contains("红包"))
        {
            return "其他";
        }
        return "其他";
    }

    private static BigDecimal parseAmount(String raw)
    {
        if (!StringUtils.hasText(raw))
        {
            return null;
        }
        Matcher m = AMOUNT.matcher(raw.replace(",", "").replace("，", "").replace("¥", "").replace("元", ""));
        if (m.find())
        {
            try
            {
                return new BigDecimal(m.group(1));
            }
            catch (Exception ignored)
            {
                return null;
            }
        }
        return null;
    }

    private static String clean(String s)
    {
        if (s == null)
        {
            return null;
        }
        String t = s.replace('\u00A0', ' ').replaceAll("[ \\t]+", " ").trim();
        return t.isEmpty() ? null : t;
    }

    private static String emptyToNull(String s)
    {
        return !StringUtils.hasText(s) ? null : s.trim();
    }

    private static String appendCell(String a, String b)
    {
        if (!StringUtils.hasText(a))
        {
            return b;
        }
        if (!StringUtils.hasText(b))
        {
            return a;
        }
        return a + b;
    }

    private static final class Word
    {
        final String text;
        final float x0;
        final float x1;
        final float y;

        Word(String text, float x0, float x1, float y)
        {
            this.text = text;
            this.x0 = x0;
            this.x1 = x1;
            this.y = y;
        }
    }

    private static final class Line
    {
        final float y;
        final List<Word> words = new ArrayList<>();
        String text = "";

        Line(float y)
        {
            this.y = y;
        }
    }
}
