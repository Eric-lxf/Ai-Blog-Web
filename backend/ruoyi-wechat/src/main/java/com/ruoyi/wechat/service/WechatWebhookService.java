package com.ruoyi.wechat.service;

public interface WechatWebhookService
{
    String verify(Long accountId, String signature, String timestamp, String nonce, String echostr,
            String msgSignature, String encryptType);

    String receive(Long accountId, String signature, String timestamp, String nonce, String requestBody,
            String msgSignature, String encryptType);
}
