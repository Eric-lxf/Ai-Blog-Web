package com.ruoyi.wechat.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.domain.WechatAccount;
import com.ruoyi.wechat.service.WechatAccountService;
import com.ruoyi.wechat.service.WechatFansService;
import com.ruoyi.wechat.service.WechatMessageService;
import com.ruoyi.wechat.service.WechatReplyService;
import com.ruoyi.wechat.service.WechatWebhookService;
import com.ruoyi.wechat.support.WechatCryptUtil;
import com.ruoyi.wechat.support.WechatSignatureUtils;
import com.ruoyi.wechat.support.WechatXmlUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatWebhookServiceImpl implements WechatWebhookService
{
    private final WechatAccountService wechatAccountService;
    private final WechatReplyService wechatReplyService;
    private final WechatMessageService wechatMessageService;
    private final WechatFansService wechatFansService;

    @Override
    public String verify(Long accountId, String signature, String timestamp, String nonce, String echostr,
            String msgSignature, String encryptType)
    {
        WechatAccount account = wechatAccountService.getEnabledAccount(accountId);
        verifySignature(account, signature, timestamp, nonce);
        if (isAesMode(encryptType, null))
        {
            WechatCryptUtil cryptUtil = createCryptUtil(account);
            return cryptUtil.decryptContent(msgSignature, timestamp, nonce, echostr);
        }
        return echostr;
    }

    @Override
    public String receive(Long accountId, String signature, String timestamp, String nonce, String requestBody,
            String msgSignature, String encryptType)
    {
        WechatAccount account = wechatAccountService.getEnabledAccount(accountId);
        verifySignature(account, signature, timestamp, nonce);
        String plainXml = resolvePlainXml(account, requestBody, msgSignature, timestamp, nonce, encryptType);
        String openId = WechatXmlUtils.readTag(plainXml, "FromUserName");
        String msgType = WechatXmlUtils.readTag(plainXml, "MsgType");
        String event = WechatXmlUtils.readTag(plainXml, "Event");
        String content = WechatXmlUtils.readTag(plainXml, "Content");
        wechatMessageService.saveInbound(accountId, openId, msgType, event, content, plainXml);
        if ("event".equalsIgnoreCase(msgType))
        {
            if ("subscribe".equalsIgnoreCase(event))
            {
                wechatFansService.handleSubscribeEvent(accountId, openId, true);
            }
            else if ("unsubscribe".equalsIgnoreCase(event))
            {
                wechatFansService.handleSubscribeEvent(accountId, openId, false);
            }
        }
        String reply = wechatReplyService.resolveReply(accountId, msgType, event, content);
        if (reply == null || reply.isBlank())
        {
            return "success";
        }
        String replyXml = buildTextReplyXml(openId, WechatXmlUtils.readTag(plainXml, "ToUserName"), reply);
        if (isAesMode(encryptType, requestBody))
        {
            return createCryptUtil(account).encrypt(replyXml);
        }
        return replyXml;
    }

    private void verifySignature(WechatAccount account, String signature, String timestamp, String nonce)
    {
        boolean ok = WechatSignatureUtils.verifySignature(account.getToken(), timestamp, nonce, signature);
        if (!ok)
        {
            throw new ServiceException("signature verification failed", HttpStatus.FORBIDDEN);
        }
    }

    private String resolvePlainXml(WechatAccount account, String requestBody, String msgSignature, String timestamp,
            String nonce, String encryptType)
    {
        if (!isAesMode(encryptType, requestBody))
        {
            return requestBody;
        }
        return createCryptUtil(account).decryptXml(msgSignature, timestamp, nonce, requestBody);
    }

    private boolean isAesMode(String encryptType, String requestBody)
    {
        return "aes".equalsIgnoreCase(encryptType) || WechatXmlUtils.containsEncryptNode(requestBody);
    }

    private WechatCryptUtil createCryptUtil(WechatAccount account)
    {
        if (!StringUtils.hasText(account.getAesKey()))
        {
            throw new ServiceException("aesKey is required for encrypted callback", HttpStatus.BAD_REQUEST);
        }
        return new WechatCryptUtil(account.getToken(), account.getAesKey(), account.getAppId());
    }

    private String buildTextReplyXml(String toUser, String fromUser, String content)
    {
        return "<xml><ToUserName><![CDATA[" + toUser + "]]></ToUserName><FromUserName><![CDATA[" + fromUser
                + "]]></FromUserName><CreateTime>" + (System.currentTimeMillis() / 1000)
                + "</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[" + content + "]]></Content></xml>";
    }
}
