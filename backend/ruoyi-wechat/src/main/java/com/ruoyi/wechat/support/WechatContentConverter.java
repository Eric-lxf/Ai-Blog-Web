package com.ruoyi.wechat.support;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WechatContentConverter
{
    public String toWechatHtml(String markdownOrHtml)
    {
        if (!StringUtils.hasText(markdownOrHtml))
        {
            return "";
        }
        return Jsoup.clean(markdownOrHtml, Safelist.relaxed());
    }

    public String toDigest(String markdownOrHtml)
    {
        if (!StringUtils.hasText(markdownOrHtml))
        {
            return "";
        }
        String plain = Jsoup.parse(markdownOrHtml).text().trim();
        if (plain.length() <= 120)
        {
            return plain;
        }
        return plain.substring(0, 120);
    }
}
