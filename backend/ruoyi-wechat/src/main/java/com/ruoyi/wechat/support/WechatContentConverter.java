package com.ruoyi.wechat.support;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WechatContentConverter
{
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private final WechatMediaUploadService wechatMediaUploadService;

    public String toWechatHtml(String markdownOrHtml, Long accountId)
    {
        if (!StringUtils.hasText(markdownOrHtml))
        {
            return "";
        }
        String html = looksLikeHtml(markdownOrHtml)
                ? markdownOrHtml
                : htmlRenderer.render(markdownParser.parse(markdownOrHtml));
        html = Jsoup.clean(html, Safelist.relaxed());
        if (accountId == null)
        {
            return html;
        }
        return replaceInlineImages(html, accountId);
    }

    public String toDigest(String markdownOrHtml)
    {
        if (!StringUtils.hasText(markdownOrHtml))
        {
            return "";
        }
        String html = looksLikeHtml(markdownOrHtml)
                ? markdownOrHtml
                : htmlRenderer.render(markdownParser.parse(markdownOrHtml));
        String plain = Jsoup.parse(html).text().trim();
        if (plain.length() <= 120)
        {
            return plain;
        }
        return plain.substring(0, 120);
    }

    private String replaceInlineImages(String html, Long accountId)
    {
        Document doc = Jsoup.parseBodyFragment(html);
        for (Element img : doc.select("img[src]"))
        {
            String src = img.attr("src");
            if (!StringUtils.hasText(src) || src.startsWith("http://mmbiz.qpic.cn")
                    || src.startsWith("https://mmbiz.qpic.cn"))
            {
                continue;
            }
            String wechatUrl = wechatMediaUploadService.uploadContentImage(accountId, src);
            img.attr("src", wechatUrl);
        }
        return doc.body().html();
    }

    private boolean looksLikeHtml(String content)
    {
        String trimmed = content.trim();
        return trimmed.startsWith("<") && trimmed.contains(">");
    }
}
