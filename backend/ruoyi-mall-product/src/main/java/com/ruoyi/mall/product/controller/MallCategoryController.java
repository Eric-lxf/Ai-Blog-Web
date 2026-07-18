package com.ruoyi.mall.product.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.mall.product.dto.MallCategoryQuery;
import com.ruoyi.mall.product.dto.MallCategorySaveRequest;
import com.ruoyi.mall.product.service.MallCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/category")
@RequiredArgsConstructor
public class MallCategoryController extends MallProductControllerSupport
{
    private final MallCategoryService mallCategoryService;

    @PreAuthorize("@ss.hasPermi('mall:category:list')")
    @GetMapping
    public AjaxResult list(MallCategoryQuery query)
    {
        if (Boolean.TRUE.equals(query.getTree()))
        {
            return AjaxResult.success(mallCategoryService.tree(query));
        }
        return AjaxResult.success(mallCategoryService.list(query));
    }

    /** 商品页下拉：树形正常类目；允许仅有商品权限的运营访问 */
    @PreAuthorize("@ss.hasPermi('mall:category:list') or @ss.hasPermi('mall:spu:list') or @ss.hasPermi('mall:spu:add') or @ss.hasPermi('mall:spu:edit')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        MallCategoryQuery query = new MallCategoryQuery();
        query.setStatus("0");
        return AjaxResult.success(mallCategoryService.tree(query));
    }

    @PreAuthorize("@ss.hasPermi('mall:category:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallCategoryService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:category:add')")
    @Log(title = "商城类目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody MallCategorySaveRequest request)
    {
        return AjaxResult.success(mallCategoryService.create(request));
    }

    @PreAuthorize("@ss.hasPermi('mall:category:edit')")
    @Log(title = "商城类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody MallCategorySaveRequest request)
    {
        mallCategoryService.update(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:category:remove')")
    @Log(title = "商城类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        mallCategoryService.delete(id);
        return AjaxResult.success();
    }
}
