package com.ruoyi.wechat.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;
import com.ruoyi.wechat.domain.WechatQrcode;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatQrcodeCreateRequest;
import com.ruoyi.wechat.mapper.WechatQrcodeMapper;
import com.ruoyi.wechat.service.WechatQrcodeService;
import com.ruoyi.wechat.support.WechatApiClient;
import com.ruoyi.wechat.support.WechatApiErrors;
import com.ruoyi.wechat.support.WechatQrcodeUtils;
import com.ruoyi.wechat.support.WechatTokenService;
import com.ruoyi.wechat.vo.WechatQrcodeVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatQrcodeServiceImpl implements WechatQrcodeService
{
    private final WechatQrcodeMapper wechatQrcodeMapper;
    private final WechatTokenService wechatTokenService;
    private final WechatApiClient wechatApiClient;

    @Override
    public Page<WechatQrcodeVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatQrcode> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatQrcode> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatQrcode::getAccountId, query.getAccountId());
        }
        if (StringUtils.hasText(query.getKeyword()))
        {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(WechatQrcode::getName, keyword)
                    .or().like(WechatQrcode::getSceneStr, keyword)
                    .or().eq(WechatQrcode::getSceneId, parseSceneId(keyword)));
        }
        wrapper.orderByDesc(WechatQrcode::getUpdateTime);
        Page<WechatQrcode> result = wechatQrcodeMapper.selectPage(page, wrapper);
        Page<WechatQrcodeVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public Long create(WechatQrcodeCreateRequest request)
    {
        QrCreateSpec spec = buildCreateSpec(request);
        ensureSceneUnique(request.getAccountId(), spec);

        String token = wechatTokenService.getAccessToken(request.getAccountId());
        String url = WechatConstants.API_HOST + "/cgi-bin/qrcode/create?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, spec.payload());
        WechatApiErrors.assertOk(resp, "create qrcode");

        Object ticketObj = resp.get("ticket");
        if (ticketObj == null)
        {
            throw new ServiceException("create qrcode failed: ticket missing", HttpStatus.ERROR);
        }

        WechatQrcode entity = new WechatQrcode();
        entity.setAccountId(request.getAccountId());
        entity.setName(request.getName().trim());
        entity.setQrType(spec.qrType());
        entity.setSceneType(spec.sceneType());
        entity.setSceneId(spec.sceneId());
        entity.setSceneStr(spec.sceneStr());
        entity.setActionName(spec.actionName());
        entity.setTicket(String.valueOf(ticketObj));
        entity.setUrl(resp.get("url") == null ? null : String.valueOf(resp.get("url")));
        entity.setExpireSeconds(spec.expireSeconds());
        entity.setExpireTime(spec.expireTime());
        entity.setScanCount(0);
        entity.setRemark(request.getRemark());
        wechatQrcodeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        if (wechatQrcodeMapper.deleteById(id) == 0)
        {
            throw new ServiceException("qrcode not found", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void recordScan(Long accountId, String event, String eventKey)
    {
        if (accountId == null || !StringUtils.hasText(eventKey))
        {
            return;
        }
        String scene = "SCAN".equalsIgnoreCase(event)
                ? eventKey
                : WechatQrcodeUtils.parseSceneFromSubscribeEventKey(eventKey);
        if (!StringUtils.hasText(scene))
        {
            return;
        }
        WechatQrcode matched = findByScene(accountId, scene);
        if (matched == null)
        {
            return;
        }
        matched.setScanCount((matched.getScanCount() == null ? 0 : matched.getScanCount()) + 1);
        wechatQrcodeMapper.updateById(matched);
    }

    private WechatQrcode findByScene(Long accountId, String scene)
    {
        Integer sceneId = parseSceneId(scene);
        if (sceneId != null)
        {
            WechatQrcode byId = wechatQrcodeMapper.selectOne(new LambdaQueryWrapper<WechatQrcode>()
                    .eq(WechatQrcode::getAccountId, accountId)
                    .eq(WechatQrcode::getSceneType, "int")
                    .eq(WechatQrcode::getSceneId, sceneId)
                    .last("limit 1"));
            if (byId != null)
            {
                return byId;
            }
        }
        return wechatQrcodeMapper.selectOne(new LambdaQueryWrapper<WechatQrcode>()
                .eq(WechatQrcode::getAccountId, accountId)
                .eq(WechatQrcode::getSceneType, "str")
                .eq(WechatQrcode::getSceneStr, scene)
                .last("limit 1"));
    }

    private void ensureSceneUnique(Long accountId, QrCreateSpec spec)
    {
        LambdaQueryWrapper<WechatQrcode> wrapper = new LambdaQueryWrapper<WechatQrcode>()
                .eq(WechatQrcode::getAccountId, accountId);
        if ("int".equals(spec.sceneType()))
        {
            wrapper.eq(WechatQrcode::getSceneType, "int").eq(WechatQrcode::getSceneId, spec.sceneId());
        }
        else
        {
            wrapper.eq(WechatQrcode::getSceneType, "str").eq(WechatQrcode::getSceneStr, spec.sceneStr());
        }
        if (wechatQrcodeMapper.selectCount(wrapper) > 0)
        {
            throw new ServiceException("scene value already exists for this account", HttpStatus.BAD_REQUEST);
        }
    }

    private QrCreateSpec buildCreateSpec(WechatQrcodeCreateRequest request)
    {
        boolean temp = "temp".equalsIgnoreCase(request.getQrType());
        boolean permanent = "permanent".equalsIgnoreCase(request.getQrType());
        if (!temp && !permanent)
        {
            throw new ServiceException("qrType must be temp or permanent", HttpStatus.BAD_REQUEST);
        }
        boolean intScene = "int".equalsIgnoreCase(request.getSceneType());
        boolean strScene = "str".equalsIgnoreCase(request.getSceneType());
        if (!intScene && !strScene)
        {
            throw new ServiceException("sceneType must be int or str", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> scene = new LinkedHashMap<>();
        String actionName;
        Integer sceneId = null;
        String sceneStr = null;
        Integer expireSeconds = null;
        LocalDateTime expireTime = null;

        if (intScene)
        {
            if (request.getSceneId() == null)
            {
                throw new ServiceException("sceneId is required for int scene", HttpStatus.BAD_REQUEST);
            }
            sceneId = request.getSceneId();
            scene.put("scene_id", sceneId);
            actionName = temp ? "QR_SCENE" : "QR_LIMIT_SCENE";
        }
        else
        {
            if (!StringUtils.hasText(request.getSceneStr()))
            {
                throw new ServiceException("sceneStr is required for str scene", HttpStatus.BAD_REQUEST);
            }
            sceneStr = request.getSceneStr().trim();
            scene.put("scene_str", sceneStr);
            actionName = temp ? "QR_STR_SCENE" : "QR_LIMIT_STR_SCENE";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action_name", actionName);
        payload.put("action_info", Map.of("scene", scene));
        if (temp)
        {
            expireSeconds = request.getExpireSeconds() == null ? 604800 : request.getExpireSeconds();
            payload.put("expire_seconds", expireSeconds);
            expireTime = LocalDateTime.now().plusSeconds(expireSeconds);
        }

        return new QrCreateSpec(temp ? "temp" : "permanent", intScene ? "int" : "str", sceneId, sceneStr, actionName,
                expireSeconds, expireTime, payload);
    }

    private Integer parseSceneId(String value)
    {
        if (!StringUtils.hasText(value))
        {
            return null;
        }
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private WechatQrcodeVO toVO(WechatQrcode entity)
    {
        WechatQrcodeVO vo = new WechatQrcodeVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setImageUrl(WechatQrcodeUtils.buildImageUrl(entity.getTicket()));
        return vo;
    }

    private record QrCreateSpec(String qrType, String sceneType, Integer sceneId, String sceneStr, String actionName,
            Integer expireSeconds, LocalDateTime expireTime, Map<String, Object> payload)
    {
    }
}
