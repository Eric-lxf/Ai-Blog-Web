package com.ruoyi.wechat.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WechatQrcodeUtils
{
    private static final String SHOW_QRCODE_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";

    private WechatQrcodeUtils()
    {
    }

    public static String buildImageUrl(String ticket)
    {
        if (ticket == null || ticket.isBlank())
        {
            return "";
        }
        return SHOW_QRCODE_URL + URLEncoder.encode(ticket, StandardCharsets.UTF_8);
    }

    /**
     * 从关注事件 EventKey 中解析场景值，形如 qrscene_123 / qrscene_abc。
     */
    public static String parseSceneFromSubscribeEventKey(String eventKey)
    {
        if (eventKey == null)
        {
            return "";
        }
        if (eventKey.startsWith("qrscene_"))
        {
            return eventKey.substring("qrscene_".length());
        }
        return eventKey;
    }
}
