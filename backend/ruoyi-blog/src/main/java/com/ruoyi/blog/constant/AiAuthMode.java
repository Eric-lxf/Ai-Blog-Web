package com.ruoyi.blog.constant;

/**
 * Anthropic Claude 鉴权方式。
 * <ul>
 *   <li>{@link #API_KEY} — 控制台 API Key，请求头 {@code x-api-key}</li>
 *   <li>{@link #AUTH_TOKEN} — 等同 {@code ANTHROPIC_AUTH_TOKEN}，请求头 {@code Authorization: Bearer}</li>
 * </ul>
 * OpenAI 兼容类型忽略此字段，始终使用 Bearer。
 */
public final class AiAuthMode
{
    public static final String API_KEY = "api_key";

    public static final String AUTH_TOKEN = "auth_token";

    private AiAuthMode()
    {
    }

    public static boolean isAuthToken(String mode)
    {
        return AUTH_TOKEN.equalsIgnoreCase(mode);
    }

    public static String normalize(String providerType, String mode)
    {
        if (!AiProviderType.isAnthropic(providerType))
        {
            return API_KEY;
        }
        if (isAuthToken(mode))
        {
            return AUTH_TOKEN;
        }
        return API_KEY;
    }
}
