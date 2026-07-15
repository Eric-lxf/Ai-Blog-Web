package com.ruoyi.blog.constant;

/**
 * AI 服务商协议类型。
 * <ul>
 *   <li>{@link #OPENAI_COMPATIBLE} — OpenAI / ChatGPT / DeepSeek / 通义兼容等，Bearer + /v1/chat/completions</li>
 *   <li>{@link #ANTHROPIC} — Claude，x-api-key + /v1/messages</li>
 * </ul>
 */
public final class AiProviderType
{
    public static final String OPENAI_COMPATIBLE = "openai_compatible";

    public static final String ANTHROPIC = "anthropic";

    public static final String AUTH_MODE_API_KEY = "api_key";

    public static final String AUTH_MODE_AUTH_TOKEN = "auth_token";

    private AiProviderType()
    {
    }

    public static boolean isOpenAiCompatible(String type)
    {
        return OPENAI_COMPATIBLE.equalsIgnoreCase(type);
    }

    public static boolean isAnthropic(String type)
    {
        return ANTHROPIC.equalsIgnoreCase(type);
    }

    public static boolean isSupported(String type)
    {
        return isOpenAiCompatible(type) || isAnthropic(type);
    }

    public static boolean isSupportedAnthropicAuthMode(String authMode)
    {
        return AUTH_MODE_API_KEY.equalsIgnoreCase(authMode) || AUTH_MODE_AUTH_TOKEN.equalsIgnoreCase(authMode);
    }
}
