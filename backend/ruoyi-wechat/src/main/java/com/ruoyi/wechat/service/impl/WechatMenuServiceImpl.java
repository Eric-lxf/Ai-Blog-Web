package com.ruoyi.wechat.service.impl;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatMenu;
import com.ruoyi.wechat.dto.WechatMenuSaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatMenuMapper;
import com.ruoyi.wechat.service.WechatMenuService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatMenuPayloadParser;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatMenuVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatMenuServiceImpl implements WechatMenuService
{
    private final WechatMenuMapper wechatMenuMapper;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public Page<WechatMenuVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatMenu> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatMenu> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatMenu::getAccountId, query.getAccountId());
        }
        wrapper.orderByDesc(WechatMenu::getUpdateTime);
        Page<WechatMenu> result = wechatMenuMapper.selectPage(page, wrapper);
        Page<WechatMenuVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public Long save(WechatMenuSaveRequest request)
    {
        WechatMenuPayloadParser.parseCreatePayload(request.getMenuJson(), objectMapper);
        WechatMenu menu = new WechatMenu();
        BeanUtils.copyProperties(request, menu);
        if (menu.getId() == null)
        {
            menu.setIsPublished(0);
            wechatMenuMapper.insert(menu);
        }
        else
        {
            if (wechatMenuMapper.updateById(menu) == 0)
            {
                throw new ServiceException("menu not found", HttpStatus.NOT_FOUND);
            }
        }
        return menu.getId();
    }

    @Override
    @Transactional
    public void publish(Long menuId)
    {
        WechatMenu menu = wechatMenuMapper.selectById(menuId);
        if (menu == null)
        {
            throw new ServiceException("menu not found", HttpStatus.NOT_FOUND);
        }
        String token = wechatTokenService.getAccessToken(menu.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/menu/create?access_token=" + token;
        Map<String, Object> payload = WechatMenuPayloadParser.parseCreatePayload(menu.getMenuJson(), objectMapper);
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "publish wechat menu");
        menu.setIsPublished(1);
        wechatMenuMapper.updateById(menu);
    }

    @Override
    public Map<String, Object> getFromWechat(Long accountId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/menu/get?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "query wechat menu");
        return resp;
    }

    @Override
    public void deleteFromWechat(Long accountId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/menu/delete?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.getJson(url);
        WechatApiErrors.assertOk(resp, "delete wechat menu");
    }

    @Override
    @Transactional
    public void delete(Long menuId)
    {
        if (wechatMenuMapper.deleteById(menuId) == 0)
        {
            throw new ServiceException("menu not found", HttpStatus.NOT_FOUND);
        }
    }

    private WechatMenuVO toVO(WechatMenu menu)
    {
        WechatMenuVO vo = new WechatMenuVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }
}
