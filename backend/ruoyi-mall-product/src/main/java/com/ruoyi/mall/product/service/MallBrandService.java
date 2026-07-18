package com.ruoyi.mall.product.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.mall.product.domain.MallBrand;
import com.ruoyi.mall.product.dto.MallBrandPageQuery;
import com.ruoyi.mall.product.dto.MallBrandSaveRequest;

public interface MallBrandService
{
    Page<MallBrand> page(MallBrandPageQuery query);

    /** 下拉选项：全部正常状态品牌（不分页） */
    List<MallBrand> listActive();

    MallBrand getById(Long id);

    Long create(MallBrandSaveRequest request);

    void update(MallBrandSaveRequest request);

    void delete(Long id);
}
