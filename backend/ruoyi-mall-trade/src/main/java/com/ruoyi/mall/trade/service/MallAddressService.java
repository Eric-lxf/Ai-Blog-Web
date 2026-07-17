package com.ruoyi.mall.trade.service;

import java.util.List;

import com.ruoyi.mall.trade.domain.MallAddress;
import com.ruoyi.mall.trade.dto.AddressSaveRequest;

public interface MallAddressService
{
    List<MallAddress> listMine();

    MallAddress getMine(Long id);

    Long create(AddressSaveRequest request);

    void update(Long id, AddressSaveRequest request);

    void delete(Long id);
}
