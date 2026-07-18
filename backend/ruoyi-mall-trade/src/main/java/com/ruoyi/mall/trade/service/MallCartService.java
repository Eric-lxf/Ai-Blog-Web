package com.ruoyi.mall.trade.service;

import java.util.List;

import com.ruoyi.mall.trade.dto.CartAddRequest;
import com.ruoyi.mall.trade.dto.CartUpdateRequest;
import com.ruoyi.mall.trade.vo.CartVO;

public interface MallCartService
{
    List<CartVO> listMine();

    Long add(CartAddRequest request);

    void update(Long id, CartUpdateRequest request);

    void delete(Long id);

    void deleteBatch(List<Long> ids);
}
