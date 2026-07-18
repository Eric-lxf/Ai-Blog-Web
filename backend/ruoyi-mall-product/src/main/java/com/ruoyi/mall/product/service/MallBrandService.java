package com.ruoyi.mall.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.mall.product.domain.MallBrand;
import com.ruoyi.mall.product.dto.MallBrandPageQuery;
import com.ruoyi.mall.product.dto.MallBrandSaveRequest;

public interface MallBrandService
{
    Page<MallBrand> page(MallBrandPageQuery query);

    MallBrand getById(Long id);

    Long create(MallBrandSaveRequest request);

    void update(MallBrandSaveRequest request);

    void delete(Long id);
}
