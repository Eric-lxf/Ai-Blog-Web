package com.ruoyi.wechat.constant;

public final class WechatConstants
{
    private WechatConstants()
    {
    }

    public static final String API_HOST = "https://api.weixin.qq.com";
    public static final String TOKEN_KEY_PREFIX = "wechat:token:";
    public static final int PUBLISH_STATUS_PENDING = 0;
    public static final int PUBLISH_STATUS_DRAFT_OK = 1;
    public static final int PUBLISH_STATUS_PUBLISHING = 2;
    public static final int PUBLISH_STATUS_PUBLISHED = 3;
    public static final int PUBLISH_STATUS_FAILED = 4;
    public static final String PUSH_MODE_DRAFT = "draft";
    public static final String PUSH_MODE_DRAFT_AND_PUBLISH = "draft_and_publish";
}
