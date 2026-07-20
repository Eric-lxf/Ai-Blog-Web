package com.ruoyi.mall.product.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.mall.product.domain.MallAttr;
import com.ruoyi.mall.product.domain.MallAttrOption;
import com.ruoyi.mall.product.domain.MallCategoryAttr;
import com.ruoyi.mall.product.dto.MallAttrOptionSaveRequest;
import com.ruoyi.mall.product.dto.MallAttrPageQuery;
import com.ruoyi.mall.product.dto.MallAttrSaveRequest;
import com.ruoyi.mall.product.dto.MallCategoryAttrBindRequest;
import com.ruoyi.mall.product.vo.MallAttrTemplateVO;

public interface MallAttrService
{
    Page<MallAttr> page(MallAttrPageQuery query);

    List<MallAttr> list(MallAttrPageQuery query);

    MallAttr get(Long id);

    Long create(MallAttrSaveRequest request);

    void update(MallAttrSaveRequest request);

    void delete(Long id);

    List<MallAttrOption> listOptions(Long attrId);

    void replaceOptions(Long attrId, List<MallAttrOptionSaveRequest> options);

    List<MallCategoryAttr> listCategoryAttrs(Long categoryId);

    void replaceCategoryAttrs(Long categoryId, List<MallCategoryAttrBindRequest.Item> items);

    MallAttrTemplateVO getAttrTemplate(Long categoryId);
}
