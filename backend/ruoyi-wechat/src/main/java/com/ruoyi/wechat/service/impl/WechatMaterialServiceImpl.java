package com.ruoyi.wechat.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatMaterialMapper;
import com.ruoyi.wechat.service.WechatMaterialService;
import com.ruoyi.wechat.vo.WechatMaterialVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatMaterialServiceImpl implements WechatMaterialService
{
    private final WechatMaterialMapper wechatMaterialMapper;

    @Override
    public Page<WechatMaterialVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatMaterial> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatMaterial> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatMaterial::getAccountId, query.getAccountId());
        }
        wrapper.orderByDesc(WechatMaterial::getUpdateTime);
        Page<WechatMaterial> result = wechatMaterialMapper.selectPage(page, wrapper);
        Page<WechatMaterialVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public Long save(WechatMaterial material)
    {
        if (material.getId() == null)
        {
            wechatMaterialMapper.insert(material);
        }
        else
        {
            if (wechatMaterialMapper.updateById(material) == 0)
            {
                throw new ServiceException("material not found", HttpStatus.NOT_FOUND);
            }
        }
        return material.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        if (wechatMaterialMapper.deleteById(id) == 0)
        {
            throw new ServiceException("material not found", HttpStatus.NOT_FOUND);
        }
    }

    private WechatMaterialVO toVO(WechatMaterial material)
    {
        WechatMaterialVO vo = new WechatMaterialVO();
        BeanUtils.copyProperties(material, vo);
        return vo;
    }
}
