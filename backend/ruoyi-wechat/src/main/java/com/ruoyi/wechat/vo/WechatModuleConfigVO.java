package com.ruoyi.wechat.vo;

import lombok.Data;

@Data
public class WechatModuleConfigVO
{
    private boolean enabled;

    private String defaultAccountId;

    private boolean callbackEncrypt;
}
