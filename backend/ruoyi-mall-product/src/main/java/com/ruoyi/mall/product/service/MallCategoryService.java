package com.ruoyi.mall.product.service;

import java.util.List;

import com.ruoyi.mall.product.domain.MallCategory;
import com.ruoyi.mall.product.dto.MallCategoryQuery;
import com.ruoyi.mall.product.dto.MallCategorySaveRequest;
import com.ruoyi.mall.product.vo.MallCategoryTreeVO;

public interface MallCategoryService
{
    List<MallCategory> list(MallCategoryQuery query);

    List<MallCategoryTreeVO> tree(MallCategoryQuery query);

    List<MallCategory> listActive();

    MallCategory getById(Long id);

    Long create(MallCategorySaveRequest request);

    void update(MallCategorySaveRequest request);

    void delete(Long id);
}
