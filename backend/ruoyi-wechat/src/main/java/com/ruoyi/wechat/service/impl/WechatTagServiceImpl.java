package com.ruoyi.wechat.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatFans;
import com.ruoyi.wechat.domain.WechatFansTag;
import com.ruoyi.wechat.domain.WechatTag;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatTagMarkRequest;
import com.ruoyi.wechat.dto.WechatTagSaveRequest;
import com.ruoyi.wechat.mapper.WechatFansMapper;
import com.ruoyi.wechat.mapper.WechatFansTagMapper;
import com.ruoyi.wechat.mapper.WechatTagMapper;
import com.ruoyi.wechat.service.WechatTagService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatTagSyncResultVO;
import com.ruoyi.wechat.vo.WechatTagVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatTagServiceImpl implements WechatTagService
{
    private final WechatTagMapper wechatTagMapper;
    private final WechatFansMapper wechatFansMapper;
    private final WechatFansTagMapper wechatFansTagMapper;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;

    @Override
    public Page<WechatTagVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatTag> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatTag> wrapper = buildQueryWrapper(query);
        Page<WechatTag> result = wechatTagMapper.selectPage(page, wrapper);
        Page<WechatTagVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public List<WechatTagVO> listByAccount(Long accountId)
    {
        if (accountId == null)
        {
            return List.of();
        }
        LambdaQueryWrapper<WechatTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WechatTag::getAccountId, accountId).orderByAsc(WechatTag::getWechatTagId);
        return wechatTagMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public Long save(WechatTagSaveRequest request)
    {
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        if (request.getId() == null)
        {
            String url = WechatConstants.API_HOST + "/cgi-bin/tags/create?access_token=" + token;
            Map<String, Object> payload = Map.of("tag", Map.of("name", request.getName().trim()));
            Map<String, Object> resp = wechatApiClient.postJson(url, payload);
            WechatApiErrors.assertOk(resp, "create wechat tag");
            Object tagObj = resp.get("tag");
            if (!(tagObj instanceof Map<?, ?> tagMap))
            {
                throw new ServiceException("create wechat tag failed: tag missing", HttpStatus.ERROR);
            }
            Integer wechatTagId = parseInt(tagMap.get("id"));
            WechatTag entity = new WechatTag();
            entity.setAccountId(request.getAccountId());
            entity.setWechatTagId(wechatTagId);
            entity.setName(request.getName().trim());
            entity.setFanCount(0);
            wechatTagMapper.insert(entity);
            return entity.getId();
        }
        WechatTag entity = wechatTagMapper.selectById(request.getId());
        if (entity == null)
        {
            throw new ServiceException("tag not found", HttpStatus.NOT_FOUND);
        }
        String url = WechatConstants.API_HOST + "/cgi-bin/tags/update?access_token=" + token;
        Map<String, Object> payload = Map.of("tag", Map.of("id", entity.getWechatTagId(), "name", request.getName().trim()));
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "update wechat tag");
        entity.setName(request.getName().trim());
        wechatTagMapper.updateById(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        WechatTag entity = wechatTagMapper.selectById(id);
        if (entity == null)
        {
            throw new ServiceException("tag not found", HttpStatus.NOT_FOUND);
        }
        String token = wechatTokenService.getAccessToken(entity.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/tags/delete?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("tag", Map.of("id", entity.getWechatTagId())));
        WechatApiErrors.assertOk(resp, "delete wechat tag");
        wechatFansTagMapper.delete(new LambdaQueryWrapper<WechatFansTag>().eq(WechatFansTag::getTagId, id));
        wechatTagMapper.deleteById(id);
    }

    @Override
    @Transactional
    public WechatTagSyncResultVO syncFromWechat(Long accountId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/tags/get?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "fetch wechat tags");
        Object tagsObj = resp.get("tags");
        if (!(tagsObj instanceof List<?> tags))
        {
            WechatTagSyncResultVO empty = new WechatTagSyncResultVO();
            empty.setAccountId(accountId);
            empty.setSynced(0);
            return empty;
        }
        Set<Integer> remoteIds = new HashSet<>();
        int synced = 0;
        for (Object item : tags)
        {
            if (!(item instanceof Map<?, ?> tagMap))
            {
                continue;
            }
            Integer wechatTagId = parseInt(tagMap.get("id"));
            String name = stringValue(tagMap.get("name"));
            Integer count = parseInt(tagMap.get("count"));
            if (wechatTagId == null || !StringUtils.hasText(name))
            {
                continue;
            }
            remoteIds.add(wechatTagId);
            WechatTag entity = wechatTagMapper.selectOne(new LambdaQueryWrapper<WechatTag>()
                    .eq(WechatTag::getAccountId, accountId).eq(WechatTag::getWechatTagId, wechatTagId));
            if (entity == null)
            {
                entity = new WechatTag();
                entity.setAccountId(accountId);
                entity.setWechatTagId(wechatTagId);
                entity.setName(name);
                entity.setFanCount(count == null ? 0 : count);
                wechatTagMapper.insert(entity);
            }
            else
            {
                entity.setName(name);
                entity.setFanCount(count == null ? 0 : count);
                wechatTagMapper.updateById(entity);
            }
            synced++;
        }
        List<WechatTag> locals = wechatTagMapper.selectList(new LambdaQueryWrapper<WechatTag>().eq(WechatTag::getAccountId, accountId));
        for (WechatTag local : locals)
        {
            if (!remoteIds.contains(local.getWechatTagId()))
            {
                wechatFansTagMapper.delete(new LambdaQueryWrapper<WechatFansTag>().eq(WechatFansTag::getTagId, local.getId()));
                wechatTagMapper.deleteById(local.getId());
            }
        }
        WechatTagSyncResultVO result = new WechatTagSyncResultVO();
        result.setAccountId(accountId);
        result.setSynced(synced);
        return result;
    }

    @Override
    @Transactional
    public void batchMark(WechatTagMarkRequest request)
    {
        WechatTag tag = wechatTagMapper.selectById(request.getTagId());
        if (tag == null || !tag.getAccountId().equals(request.getAccountId()))
        {
            throw new ServiceException("tag not found", HttpStatus.NOT_FOUND);
        }
        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String apiPath = Boolean.TRUE.equals(request.getMark()) ? "/cgi-bin/tags/members/batchtagging"
                : "/cgi-bin/tags/members/batchuntagging";
        String url = WechatConstants.API_HOST + apiPath + "?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("openid_list", request.getOpenIds());
        payload.put("tagid", tag.getWechatTagId());
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "batch mark fans tag");
        refreshLocalFanTags(request.getAccountId(), tag, request.getOpenIds(), Boolean.TRUE.equals(request.getMark()));
    }

    @Override
    public void applyFanTagsFromUserInfo(Long accountId, Long fansId, Object tagIdListObj)
    {
        if (accountId == null || fansId == null)
        {
            return;
        }
        List<Integer> wechatTagIds = new ArrayList<>();
        if (tagIdListObj instanceof List<?> list)
        {
            for (Object item : list)
            {
                Integer tagId = parseInt(item);
                if (tagId != null)
                {
                    wechatTagIds.add(tagId);
                }
            }
        }
        applyFanTags(accountId, fansId, wechatTagIds);
    }

    private void applyFanTags(Long accountId, Long fansId, List<Integer> wechatTagIds)
    {
        wechatFansTagMapper.delete(new LambdaQueryWrapper<WechatFansTag>().eq(WechatFansTag::getFansId, fansId));
        if (wechatTagIds.isEmpty())
        {
            return;
        }
        List<WechatTag> tags = wechatTagMapper.selectList(new LambdaQueryWrapper<WechatTag>()
                .eq(WechatTag::getAccountId, accountId).in(WechatTag::getWechatTagId, wechatTagIds));
        for (WechatTag tag : tags)
        {
            WechatFansTag relation = new WechatFansTag();
            relation.setFansId(fansId);
            relation.setTagId(tag.getId());
            wechatFansTagMapper.insert(relation);
        }
    }

    private void refreshLocalFanTags(Long accountId, WechatTag tag, List<String> openIds, boolean mark)
    {
        for (String openId : openIds)
        {
            WechatFans fan = wechatFansMapper.selectOne(new LambdaQueryWrapper<WechatFans>()
                    .eq(WechatFans::getAccountId, accountId).eq(WechatFans::getOpenId, openId));
            if (fan == null)
            {
                continue;
            }
            if (mark)
            {
                Long count = wechatFansTagMapper.selectCount(new LambdaQueryWrapper<WechatFansTag>()
                        .eq(WechatFansTag::getFansId, fan.getId()).eq(WechatFansTag::getTagId, tag.getId()));
                if (count == null || count == 0)
                {
                    WechatFansTag relation = new WechatFansTag();
                    relation.setFansId(fan.getId());
                    relation.setTagId(tag.getId());
                    wechatFansTagMapper.insert(relation);
                }
            }
            else
            {
                wechatFansTagMapper.delete(new LambdaQueryWrapper<WechatFansTag>()
                        .eq(WechatFansTag::getFansId, fan.getId()).eq(WechatFansTag::getTagId, tag.getId()));
            }
        }
    }

    private LambdaQueryWrapper<WechatTag> buildQueryWrapper(WechatPageQuery query)
    {
        LambdaQueryWrapper<WechatTag> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatTag::getAccountId, query.getAccountId());
        }
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.like(WechatTag::getName, query.getKeyword().trim());
        }
        wrapper.orderByAsc(WechatTag::getWechatTagId);
        return wrapper;
    }

    private WechatTagVO toVO(WechatTag source)
    {
        WechatTagVO vo = new WechatTagVO();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }

    private Integer parseInt(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return Integer.parseInt(String.valueOf(value));
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
}
