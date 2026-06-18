package com.ruoyi.wechat.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.dto.WechatTemplateSendRequest;
import com.ruoyi.wechat.service.WechatMessageService;
import com.ruoyi.wechat.service.WechatTemplateMessageService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatPrivateTemplateVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatTemplateMessageServiceImpl implements WechatTemplateMessageService
{
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;
    private final WechatMessageService wechatMessageService;

    @Override
    public List<WechatPrivateTemplateVO> listTemplates(Long accountId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/template/get_all_private_template?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "fetch private templates");
        Object listObj = resp.get("template_list");
        if (!(listObj instanceof List<?> templates))
        {
            return List.of();
        }
        List<WechatPrivateTemplateVO> result = new ArrayList<>();
        for (Object item : templates)
        {
            if (!(item instanceof Map<?, ?> map))
            {
                continue;
            }
            WechatPrivateTemplateVO vo = new WechatPrivateTemplateVO();
            vo.setTemplateId(stringValue(map.get("template_id")));
            vo.setTitle(stringValue(map.get("title")));
            vo.setPrimaryIndustry(stringValue(map.get("primary_industry")));
            vo.setDeputyIndustry(stringValue(map.get("deputy_industry")));
            vo.setContent(stringValue(map.get("content")));
            vo.setExample(stringValue(map.get("example")));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public void send(WechatTemplateSendRequest request)
    {
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/message/template/send?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("touser", request.getOpenId());
        payload.put("template_id", request.getTemplateId());
        if (StringUtils.hasText(request.getUrl()))
        {
            payload.put("url", request.getUrl().trim());
        }
        payload.put("data", request.getData());
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "send template message");
        wechatMessageService.saveOutbound(request.getAccountId(), request.getOpenId(), "template",
                "templateId=" + request.getTemplateId());
    }

    private String stringValue(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }
}
