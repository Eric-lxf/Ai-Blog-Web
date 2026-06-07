package com.ruoyi.wechat.service.impl;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.domain.WechatAccount;
import com.ruoyi.wechat.service.WechatAccountService;
import com.ruoyi.wechat.service.WechatFansService;
import com.ruoyi.wechat.service.WechatMessageService;
import com.ruoyi.wechat.service.WechatReplyService;
import com.ruoyi.wechat.service.WechatWebhookService;
import com.ruoyi.wechat.support.WechatSignatureUtils;

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
    public String verify(Long accountId, String signature, String timestamp, String nonce, String echostr)
    {
        WechatAccount account = wechatAccountService.getEnabledAccount(accountId);
        boolean ok = WechatSignatureUtils.verifySignature(account.getToken(), timestamp, nonce, signature);
        if (!ok)
        {
            throw new ServiceException("signature verification failed", HttpStatus.FORBIDDEN);
        }
        return echostr;
    }

    @Override
    public String receive(Long accountId, String signature, String timestamp, String nonce, String requestBody)
    {
        WechatAccount account = wechatAccountService.getEnabledAccount(accountId);
        boolean ok = WechatSignatureUtils.verifySignature(account.getToken(), timestamp, nonce, signature);
        if (!ok)
        {
            throw new ServiceException("signature verification failed", HttpStatus.FORBIDDEN);
        }
        String openId = readXmlTag(requestBody, "FromUserName");
        String msgType = readXmlTag(requestBody, "MsgType");
        String event = readXmlTag(requestBody, "Event");
        String content = readXmlTag(requestBody, "Content");
        wechatMessageService.saveInbound(accountId, openId, msgType, event, content, requestBody);
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
        return buildTextReplyXml(openId, readXmlTag(requestBody, "ToUserName"), reply);
    }

    private String buildTextReplyXml(String toUser, String fromUser, String content)
    {
        return "<xml><ToUserName><![CDATA[" + toUser + "]]></ToUserName><FromUserName><![CDATA[" + fromUser
                + "]]></FromUserName><CreateTime>" + (System.currentTimeMillis() / 1000)
                + "</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA[" + content + "]]></Content></xml>";
    }

    private String readXmlTag(String xml, String tag)
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList list = document.getElementsByTagName(tag);
            if (list.getLength() == 0 || list.item(0) == null)
            {
                return "";
            }
            return list.item(0).getTextContent();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
