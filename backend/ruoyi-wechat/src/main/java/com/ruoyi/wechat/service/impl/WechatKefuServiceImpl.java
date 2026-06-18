package com.ruoyi.wechat.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMessageLog;
import com.ruoyi.wechat.dto.WechatKefuSendRequest;
import com.ruoyi.wechat.mapper.WechatMessageLogMapper;
import com.ruoyi.wechat.service.WechatKefuService;
import com.ruoyi.wechat.service.WechatMessageService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatKefuSessionVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatKefuServiceImpl implements WechatKefuService
{
    private final WechatMessageLogMapper wechatMessageLogMapper;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;
    private final WechatMessageService wechatMessageService;

    @Override
    public WechatKefuSessionVO checkSession(Long accountId, String openId)
    {
        WechatKefuSessionVO vo = new WechatKefuSessionVO();
        if (accountId == null || !StringUtils.hasText(openId))
        {
            vo.setCanSend(false);
            vo.setReason("账号或 OpenID 无效");
            return vo;
        }
        LocalDateTime since = LocalDateTime.now().minusHours(48);
        WechatMessageLog latest = wechatMessageLogMapper.selectOne(new LambdaQueryWrapper<WechatMessageLog>()
                .eq(WechatMessageLog::getAccountId, accountId)
                .eq(WechatMessageLog::getOpenId, openId)
                .eq(WechatMessageLog::getDirection, "in")
                .ge(WechatMessageLog::getCreateTime, since)
                .orderByDesc(WechatMessageLog::getCreateTime)
                .last("LIMIT 1"));
        if (latest == null)
        {
            vo.setCanSend(false);
            vo.setReason("48 小时内无用户消息，无法主动发送客服消息");
            return vo;
        }
        vo.setCanSend(true);
        vo.setReason("可发送");
        return vo;
    }

    @Override
    @Transactional
    public void sendText(WechatKefuSendRequest request)
    {
        WechatKefuSessionVO session = checkSession(request.getAccountId(), request.getOpenId());
        if (!session.isCanSend())
        {
            throw new ServiceException(session.getReason());
        }
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/message/custom/send?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("touser", request.getOpenId());
        payload.put("msgtype", "text");
        payload.put("text", Map.of("content", request.getContent().trim()));
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "send kefu message");
        wechatMessageService.saveOutbound(request.getAccountId(), request.getOpenId(), "text", request.getContent().trim());
    }
}
