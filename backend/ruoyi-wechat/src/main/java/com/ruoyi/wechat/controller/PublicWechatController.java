package com.ruoyi.wechat.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.wechat.service.WechatWebhookService;

import lombok.RequiredArgsConstructor;

@Anonymous
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/wechat/callback")
public class PublicWechatController
{
    private final WechatWebhookService wechatWebhookService;

    @GetMapping(value = "/{accountId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@PathVariable Long accountId, @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr)
    {
        return wechatWebhookService.verify(accountId, signature, timestamp, nonce, echostr);
    }

    @PostMapping(value = "/{accountId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String callback(@PathVariable Long accountId, @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce, @RequestBody String body)
    {
        return wechatWebhookService.receive(accountId, signature, timestamp, nonce, body);
    }
}
