package com.ruoyi.mall.product.service;

import java.util.List;
import java.util.Set;

import com.ruoyi.mall.product.domain.MallFrontCategory;
import com.ruoyi.mall.product.domain.MallFrontCategoryRel;
import com.ruoyi.mall.product.dto.MallFrontCategoryQuery;
import com.ruoyi.mall.product.dto.MallFrontCategorySaveRequest;
import com.ruoyi.mall.product.vo.MallFrontCategoryTreeVO;

public interface MallFrontCategoryService
{
    List<MallFrontCategory> list(MallFrontCategoryQuery query);

    List<MallFrontCategoryTreeVO> tree(MallFrontCategoryQuery query);

    List<MallFrontCategoryTreeVO> listActiveTree();

    MallFrontCategory getById(Long id);

    Long create(MallFrontCategorySaveRequest request);

    void update(MallFrontCategorySaveRequest request);

    void delete(Long id);

    List<MallFrontCategoryRel> listRels(Long frontId);

    void replaceRels(Long frontId, List<Long> backCategoryIds);

    Set<Long> resolveBackCategoryIds(Long frontCategoryId);
}
