package com.ruoyi.mall.trade.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.product.service.MallSkuStockService;
import com.ruoyi.mall.product.service.dto.MallSkuInfo;
import com.ruoyi.mall.trade.domain.MallCart;
import com.ruoyi.mall.trade.dto.CartAddRequest;
import com.ruoyi.mall.trade.dto.CartUpdateRequest;
import com.ruoyi.mall.trade.mapper.MallCartMapper;
import com.ruoyi.mall.trade.service.MallCartService;
import com.ruoyi.mall.trade.vo.CartVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallCartServiceImpl implements MallCartService
{
    private static final String CHECKED_YES = "1";
    private static final String CHECKED_NO = "0";

    private final MallCartMapper mallCartMapper;
    private final MallSkuStockService mallSkuStockService;

    @Override
    public List<CartVO> listMine()
    {
        Long userId = SecurityUtils.getUserId();
        List<MallCart> carts = mallCartMapper.selectList(new LambdaQueryWrapper<MallCart>()
                .eq(MallCart::getUserId, userId)
                .orderByDesc(MallCart::getUpdateTime)
                .orderByDesc(MallCart::getId));
        return carts.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public Long add(CartAddRequest request)
    {
        Long userId = SecurityUtils.getUserId();
        MallSkuInfo sku = mallSkuStockService.getEnabledSku(request.getSkuId());
        if (sku == null)
        {
            throw new ServiceException("商品不存在或已下架");
        }
        MallCart existing = mallCartMapper.selectOne(new LambdaQueryWrapper<MallCart>()
                .eq(MallCart::getUserId, userId)
                .eq(MallCart::getSkuId, request.getSkuId()));
        if (existing != null)
        {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setChecked(CHECKED_YES);
            mallCartMapper.updateById(existing);
            return existing.getId();
        }
        MallCart cart = new MallCart();
        cart.setUserId(userId);
        cart.setSkuId(request.getSkuId());
        cart.setQuantity(request.getQuantity());
        cart.setChecked(CHECKED_YES);
        mallCartMapper.insert(cart);
        return cart.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CartUpdateRequest request)
    {
        MallCart cart = requireMine(id, SecurityUtils.getUserId());
        if (request.getQuantity() != null)
        {
            cart.setQuantity(request.getQuantity());
        }
        if (request.getChecked() != null)
        {
            cart.setChecked(CHECKED_YES.equals(request.getChecked()) ? CHECKED_YES : CHECKED_NO);
        }
        mallCartMapper.updateById(cart);
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        MallCart cart = requireMine(id, SecurityUtils.getUserId());
        mallCartMapper.deleteById(cart.getId());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids)
    {
        if (CollectionUtils.isEmpty(ids))
        {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        mallCartMapper.delete(new LambdaQueryWrapper<MallCart>()
                .eq(MallCart::getUserId, userId)
                .in(MallCart::getId, ids));
    }

    private MallCart requireMine(Long id, Long userId)
    {
        MallCart cart = mallCartMapper.selectOne(new LambdaQueryWrapper<MallCart>()
                .eq(MallCart::getId, id)
                .eq(MallCart::getUserId, userId));
        if (cart == null)
        {
            throw new ServiceException("购物车项不存在");
        }
        return cart;
    }

    private CartVO toVO(MallCart cart)
    {
        CartVO vo = new CartVO();
        vo.setId(cart.getId());
        vo.setUserId(cart.getUserId());
        vo.setSkuId(cart.getSkuId());
        vo.setQuantity(cart.getQuantity());
        vo.setChecked(cart.getChecked());
        vo.setCreateTime(cart.getCreateTime());
        vo.setUpdateTime(cart.getUpdateTime());
        MallSkuInfo sku = mallSkuStockService.getEnabledSku(cart.getSkuId());
        vo.setSkuEnabled(sku != null);
        if (sku != null)
        {
            vo.setSpuId(sku.getSpuId());
            vo.setSpuName(sku.getSpuName());
            vo.setSkuCode(sku.getSkuCode());
            vo.setSkuSpecs(sku.getSkuSpecs());
            vo.setImage(sku.getImage());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
        }
        return vo;
    }
}
