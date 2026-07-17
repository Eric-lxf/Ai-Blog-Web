package com.ruoyi.blog.service.bill;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

/**
 * 将 PDF 按页渲染：每一页生成一张独立 JPEG（不拼页），供视觉 OCR 逐页识别。
 */
public final class BillPdfRenderer
{
    private static final int MAX_PAGES = 20;
    private static final float DPI = 160f;

    private BillPdfRenderer()
    {
    }

    /**
     * @return 与 PDF 页序一致的 JPEG data URL 列表，size = min(总页数, MAX_PAGES)
     */
    public static List<String> toJpegDataUrls(InputStream in)
    {
        try (PDDocument document = PDDocument.load(in))
        {
            int pages = document.getNumberOfPages();
            if (pages <= 0)
            {
                throw new ServiceException("PDF 没有可识别的页面", HttpStatus.BAD_REQUEST);
            }
            int limit = Math.min(pages, MAX_PAGES);
            PDFRenderer renderer = new PDFRenderer(document);
            List<String> urls = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++)
            {
                // 一页一图，互不拼接
                BufferedImage image = renderer.renderImageWithDPI(i, DPI, ImageType.RGB);
                urls.add(toJpegDataUrl(image));
            }
            return urls;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("PDF 解析失败：" + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private static String toJpegDataUrl(BufferedImage image) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/jpeg;base64," + b64;
    }
}
