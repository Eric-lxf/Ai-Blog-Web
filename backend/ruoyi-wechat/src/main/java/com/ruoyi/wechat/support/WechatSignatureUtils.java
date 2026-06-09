package com.ruoyi.wechat.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public final class WechatSignatureUtils
{
    private WechatSignatureUtils()
    {
    }

    public static boolean verifySignature(String token, String timestamp, String nonce, String signature)
    {
        if (isBlank(token) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature))
        {
            return false;
        }
        return sign(token, timestamp, nonce).equalsIgnoreCase(signature);
    }

    public static String sign(String token, String timestamp, String nonce)
    {
        String[] values = { token, timestamp, nonce };
        Arrays.sort(values);
        String joined = String.join("", values);
        return sha1(joined);
    }

    public static String signMessage(String token, String timestamp, String nonce, String encrypt)
    {
        String[] values = { token, timestamp, nonce, encrypt };
        Arrays.sort(values);
        return sha1(String.join("", values));
    }

    private static String sha1(String text)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte value : bytes)
            {
                sb.append(String.format("%02x", value));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("sha1 compute failed", e);
        }
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
