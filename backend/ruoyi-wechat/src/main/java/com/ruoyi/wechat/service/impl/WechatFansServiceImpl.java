package com.ruoyi.wechat.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatFans;
import com.ruoyi.wechat.domain.WechatMessageLog;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatFansMapper;
import com.ruoyi.wechat.mapper.WechatMessageLogMapper;
import com.ruoyi.wechat.service.WechatFansService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatFansSyncResultVO;
import com.ruoyi.wechat.vo.WechatFansVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatFansServiceImpl implements WechatFansService
{
    private static final int BATCH_SIZE = 100;
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final WechatFansMapper wechatFansMapper;
    private final WechatMessageLogMapper wechatMessageLogMapper;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;

    @Override
    public Page<WechatFansVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatFans> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatFans> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatFans::getAccountId, query.getAccountId());
        }
        if (query.getStatus() != null)
        {
            wrapper.eq(WechatFans::getSubscribeStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.and(w -> w.like(WechatFans::getNickname, query.getKeyword()).or()
                    .like(WechatFans::getOpenId, query.getKeyword()));
        }
        wrapper.orderByDesc(WechatFans::getUpdateTime);
        Page<WechatFans> result = wechatFansMapper.selectPage(page, wrapper);
        Page<WechatFansVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public WechatFansSyncResultVO syncFromWechat(Long accountId)
    {
        if (accountId == null)
        {
            throw new ServiceException("accountId is required");
        }
        String token = wechatTokenService.getAccessToken(accountId);
        try
        {
            List<String> openIds = fetchAllOpenIds(token);
            int synced = 0;
            for (int i = 0; i < openIds.size(); i += BATCH_SIZE)
            {
                List<String> batch = openIds.subList(i, Math.min(i + BATCH_SIZE, openIds.size()));
                synced += upsertBatch(accountId, token, batch);
            }
            WechatFansSyncResultVO result = new WechatFansSyncResultVO();
            result.setAccountId(accountId);
            result.setTotal(openIds.size());
            result.setSynced(synced);
            result.setSource("wechat_api");
            return result;
        }
        catch (ServiceException e)
        {
            if (WechatApiErrors.isUnauthorized(e))
            {
                WechatFansSyncResultVO fallback = syncFromMessageLog(accountId);
                fallback.setWarning(WechatApiErrors.fansListUnauthorizedHint());
                return fallback;
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void handleSubscribeEvent(Long accountId, String openId, boolean subscribed)
    {
        if (accountId == null || !StringUtils.hasText(openId))
        {
            return;
        }
        if (!subscribed)
        {
            markUnsubscribed(accountId, openId);
            return;
        }
        try
        {
            String token = wechatTokenService.getAccessToken(accountId);
            Map<String, Object> user = fetchUserInfo(token, openId);
            upsertFan(accountId, user);
        }
        catch (ServiceException e)
        {
            if (WechatApiErrors.isUnauthorized(e))
            {
                upsertMinimal(accountId, openId, 1, LocalDateTime.now(CHINA_ZONE));
                return;
            }
            throw e;
        }
        catch (Exception e)
        {
            upsertMinimal(accountId, openId, 1, null);
        }
    }

    private WechatFansSyncResultVO syncFromMessageLog(Long accountId)
    {
        List<WechatMessageLog> logs = wechatMessageLogMapper.selectList(new LambdaQueryWrapper<WechatMessageLog>()
                .eq(WechatMessageLog::getAccountId, accountId).isNotNull(WechatMessageLog::getOpenId)
                .ne(WechatMessageLog::getOpenId, "").orderByAsc(WechatMessageLog::getCreateTime));
        Map<String, MessageFanState> states = new LinkedHashMap<>();
        for (WechatMessageLog log : logs)
        {
            String openId = log.getOpenId();
            MessageFanState state = states.computeIfAbsent(openId, key -> new MessageFanState());
            if ("event".equalsIgnoreCase(log.getMessageType()))
            {
                if ("subscribe".equalsIgnoreCase(log.getEventType()))
                {
                    state.subscribeStatus = 1;
                    state.subscribeTime = log.getCreateTime();
                }
                else if ("unsubscribe".equalsIgnoreCase(log.getEventType()))
                {
                    state.subscribeStatus = 0;
                }
            }
            else if (state.subscribeStatus == null)
            {
                state.subscribeStatus = 1;
            }
        }
        int synced = 0;
        for (Map.Entry<String, MessageFanState> entry : states.entrySet())
        {
            MessageFanState state = entry.getValue();
            int subscribeStatus = state.subscribeStatus == null ? 1 : state.subscribeStatus;
            upsertMinimal(accountId, entry.getKey(), subscribeStatus, state.subscribeTime);
            synced++;
        }
        WechatFansSyncResultVO result = new WechatFansSyncResultVO();
        result.setAccountId(accountId);
        result.setTotal(states.size());
        result.setSynced(synced);
        result.setSource("message_log");
        return result;
    }

    private List<String> fetchAllOpenIds(String token)
    {
        List<String> openIds = new ArrayList<>();
        String nextOpenId = "";
        do
        {
            String url = WechatConstants.API_HOST + "/cgi-bin/user/get?access_token=" + token + "&next_openid=" + nextOpenId;
            Map<String, Object> resp = wechatApiClient.getJson(url);
            WechatApiErrors.assertOk(resp, "fetch fans openid list");
            Object dataObj = resp.get("data");
            if (dataObj instanceof Map<?, ?> dataMap)
            {
                Object openidObj = dataMap.get("openid");
                if (openidObj instanceof List<?> list)
                {
                    for (Object item : list)
                    {
                        if (item != null && StringUtils.hasText(String.valueOf(item)))
                        {
                            openIds.add(String.valueOf(item));
                        }
                    }
                }
            }
            Object next = resp.get("next_openid");
            nextOpenId = next == null ? "" : String.valueOf(next);
        }
        while (StringUtils.hasText(nextOpenId));
        return openIds;
    }

    private int upsertBatch(Long accountId, String token, List<String> openIds)
    {
        if (openIds.isEmpty())
        {
            return 0;
        }
        List<Map<String, String>> userList = openIds.stream().map(openId -> Map.of("openid", openId, "lang", "zh_CN")).toList();
        String url = WechatConstants.API_HOST + "/cgi-bin/user/info/batchget?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("user_list", userList));
        try
        {
            WechatApiErrors.assertOk(resp, "batch fetch fans info");
        }
        catch (ServiceException e)
        {
            if (WechatApiErrors.isUnauthorized(e))
            {
                for (String openId : openIds)
                {
                    upsertMinimal(accountId, openId, 1, null);
                }
                return openIds.size();
            }
            throw e;
        }
        Object usersObj = resp.get("user_info_list");
        if (!(usersObj instanceof List<?> users))
        {
            return 0;
        }
        int synced = 0;
        for (Object userObj : users)
        {
            if (userObj instanceof Map<?, ?> userMap)
            {
                upsertFan(accountId, castMap(userMap));
                synced++;
            }
        }
        return synced;
    }

    private Map<String, Object> fetchUserInfo(String token, String openId)
    {
        String url = WechatConstants.API_HOST + "/cgi-bin/user/info?access_token=" + token + "&openid=" + openId + "&lang=zh_CN";
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "fetch fan info");
        return resp;
    }

    private void upsertFan(Long accountId, Map<String, Object> user)
    {
        String openId = stringValue(user.get("openid"));
        if (!StringUtils.hasText(openId))
        {
            return;
        }
        int subscribeStatus = parseSubscribeStatus(user.get("subscribe"));
        LocalDateTime subscribeTime = parseSubscribeTime(user.get("subscribe_time"), subscribeStatus);
        WechatFans entity = findByAccountAndOpenId(accountId, openId);
        if (entity == null)
        {
            entity = new WechatFans();
            entity.setAccountId(accountId);
            entity.setOpenId(openId);
        }
        entity.setUnionId(stringValue(user.get("unionid")));
        entity.setNickname(stringValue(user.get("nickname")));
        entity.setSubscribeStatus(subscribeStatus);
        entity.setSubscribeTime(subscribeTime);
        saveOrUpdate(entity);
    }

    private void upsertMinimal(Long accountId, String openId, int subscribeStatus, LocalDateTime subscribeTime)
    {
        WechatFans entity = findByAccountAndOpenId(accountId, openId);
        if (entity == null)
        {
            entity = new WechatFans();
            entity.setAccountId(accountId);
            entity.setOpenId(openId);
        }
        entity.setSubscribeStatus(subscribeStatus);
        if (subscribeTime != null)
        {
            entity.setSubscribeTime(subscribeTime);
        }
        else if (subscribeStatus == 1 && entity.getSubscribeTime() == null)
        {
            entity.setSubscribeTime(LocalDateTime.now(CHINA_ZONE));
        }
        saveOrUpdate(entity);
    }

    private void markUnsubscribed(Long accountId, String openId)
    {
        WechatFans entity = findByAccountAndOpenId(accountId, openId);
        if (entity == null)
        {
            upsertMinimal(accountId, openId, 0, null);
            return;
        }
        entity.setSubscribeStatus(0);
        wechatFansMapper.updateById(entity);
    }

    private WechatFans findByAccountAndOpenId(Long accountId, String openId)
    {
        return wechatFansMapper.selectOne(new LambdaQueryWrapper<WechatFans>().eq(WechatFans::getAccountId, accountId)
                .eq(WechatFans::getOpenId, openId));
    }

    private void saveOrUpdate(WechatFans entity)
    {
        if (entity.getId() == null)
        {
            wechatFansMapper.insert(entity);
        }
        else
        {
            wechatFansMapper.updateById(entity);
        }
    }

    private int parseSubscribeStatus(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        return "1".equals(String.valueOf(value)) ? 1 : 0;
    }

    private LocalDateTime parseSubscribeTime(Object value, int subscribeStatus)
    {
        if (subscribeStatus != 1 || value == null)
        {
            return null;
        }
        try
        {
            long epochSecond = Long.parseLong(String.valueOf(value));
            if (epochSecond <= 0)
            {
                return null;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), CHINA_ZONE);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String stringValue(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> castMap(Map<?, ?> source)
    {
        Map<String, Object> target = new HashMap<>();
        source.forEach((key, value) -> target.put(String.valueOf(key), value));
        return target;
    }

    private WechatFansVO toVO(WechatFans source)
    {
        WechatFansVO vo = new WechatFansVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private static final class MessageFanState
    {
        private Integer subscribeStatus;
        private LocalDateTime subscribeTime;
    }
}
