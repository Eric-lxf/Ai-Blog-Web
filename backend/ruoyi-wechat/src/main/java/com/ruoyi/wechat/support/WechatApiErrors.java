package com.ruoyi.wechat.support;

import java.util.Map;

import org.springframework.util.StringUtils;

import com.ruoyi.common.exception.ServiceException;

public final class WechatApiErrors
{
    public static final String UNAUTHORIZED_MARKER = "[WECHAT_API_UNAUTHORIZED]";

    private WechatApiErrors()
    {
    }

    public static Integer parseErrcode(Map<String, Object> resp)
    {
        if (resp == null)
        {
            return null;
        }
        Object errCode = resp.get("errcode");
        if (errCode == null)
        {
            return null;
        }
        try
        {
            return Integer.parseInt(String.valueOf(errCode));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static void assertOk(Map<String, Object> resp, String action)
    {
        Integer errcode = parseErrcode(resp);
        if (errcode == null || errcode == 0)
        {
            return;
        }
        String errmsg = resp.get("errmsg") == null ? "" : String.valueOf(resp.get("errmsg"));
        if (errcode == 48001)
        {
            throw unauthorized(fansListUnauthorizedHint());
        }
        String detail = StringUtils.hasText(errmsg) ? errmsg : String.valueOf(errcode);
        throw new ServiceException(action + " failed: errcode=" + errcode + ", errmsg=" + detail);
    }

    public static boolean isUnauthorized(ServiceException exception)
    {
        return exception != null && exception.getMessage() != null
                && exception.getMessage().startsWith(UNAUTHORIZED_MARKER);
    }

    public static ServiceException unauthorized(String message)
    {
        return new ServiceException(UNAUTHORIZED_MARKER + message);
    }

    public static String fansListUnauthorizedHint()
    {
        return "\u5f53\u524d\u516c\u4f17\u53f7\u65e0\u6279\u91cf\u83b7\u53d6\u7c89\u4e1d\u5217\u8868\u6743\u9650\uff08errcode=48001\uff09\u3002"
                + "\u901a\u5e38\u89c1\u4e8e\u672a\u5fae\u4fe1\u8ba4\u8bc1\u7684\u8ba2\u9605\u53f7/\u4e2a\u4eba\u53f7\u3002"
                + "\u5df2\u6539\u4e3a\u4ece\u6d88\u606f\u65e5\u5fd7\u8865\u5168\u56de\u8c03\u8bb0\u5f55\u5230\u7684\u7c89\u4e1d\uff1b"
                + "\u5b8c\u6574\u5168\u91cf\u540c\u6b65\u9700\u5b8c\u6210\u5fae\u4fe1\u8ba4\u8bc1\u6216\u4f7f\u7528\u8ba4\u8bc1\u670d\u52a1\u53f7\u3002";
    }
}
