package com.ruoyi.wechat.support;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatAccount;
import com.ruoyi.wechat.mapper.WechatAccountMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatTokenService
{
    private final RedisCache redisCache;
    private final WechatAccountMapper wechatAccountMapper;
    private final WechatApiClient wechatApiClient;

    public String getAccessToken(Long accountId)
    {
        String cacheKey = WechatConstants.TOKEN_KEY_PREFIX + accountId;
        String cached = redisCache.getCacheObject(cacheKey);
        if (StringUtils.hasText(cached))
        {
            return cached;
        }
        WechatAccount account = wechatAccountMapper.selectOne(
                new LambdaQueryWrapper<WechatAccount>().eq(WechatAccount::getId, accountId).eq(WechatAccount::getEnabled, 1));
        if (account == null)
        {
            throw new ServiceException("wechat account not found or disabled", HttpStatus.NOT_FOUND);
        }
        String url = WechatConstants.API_HOST + "/cgi-bin/token?grant_type=client_credential&appid=" + account.getAppId()
                + "&secret=" + account.getAppSecret();
        Map<String, Object> result = wechatApiClient.getJson(url);
        Object tokenObj = result.get("access_token");
        if (tokenObj == null)
        {
            throw new ServiceException("get access_token failed: " + result);
        }
        String token = String.valueOf(tokenObj);
        int expiresIn = parseInt(result.get("expires_in"), 7200);
        redisCache.setCacheObject(cacheKey, token, Math.max(60, expiresIn - 120), TimeUnit.SECONDS);
        return token;
    }

    public void clearAccessToken(Long accountId)
    {
        redisCache.deleteObject(WechatConstants.TOKEN_KEY_PREFIX + accountId);
    }

    private int parseInt(Object value, int defaultValue)
    {
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(String.valueOf(value));
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
}
