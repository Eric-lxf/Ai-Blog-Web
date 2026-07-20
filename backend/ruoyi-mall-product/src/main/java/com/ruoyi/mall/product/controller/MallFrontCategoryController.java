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
import com.ruoyi.mall.product.dto.MallFrontCategoryQuery;
import com.ruoyi.mall.product.dto.MallFrontCategoryRelReplaceRequest;
import com.ruoyi.mall.product.dto.MallFrontCategorySaveRequest;
import com.ruoyi.mall.product.service.MallFrontCategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/front-category")
@RequiredArgsConstructor
public class MallFrontCategoryController extends MallProductControllerSupport
{
    private final MallFrontCategoryService mallFrontCategoryService;

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:list')")
    @GetMapping
    public AjaxResult list(MallFrontCategoryQuery query)
    {
        if (Boolean.TRUE.equals(query.getTree()))
        {
            return AjaxResult.success(mallFrontCategoryService.tree(query));
        }
        return AjaxResult.success(mallFrontCategoryService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return AjaxResult.success(mallFrontCategoryService.listActiveTree());
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallFrontCategoryService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:add')")
    @Log(title = "商城前台类目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody MallFrontCategorySaveRequest request)
    {
        return AjaxResult.success(mallFrontCategoryService.create(request));
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:edit')")
    @Log(title = "商城前台类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody MallFrontCategorySaveRequest request)
    {
        mallFrontCategoryService.update(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:remove')")
    @Log(title = "商城前台类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        mallFrontCategoryService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:query') or @ss.hasPermi('mall:frontCategory:edit')")
    @GetMapping("/{id}/rels")
    public AjaxResult listRels(@PathVariable Long id)
    {
        return AjaxResult.success(mallFrontCategoryService.listRels(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:frontCategory:edit')")
    @Log(title = "商城前台类目映射", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/rels")
    public AjaxResult replaceRels(@PathVariable Long id, @Valid @RequestBody MallFrontCategoryRelReplaceRequest request)
    {
        mallFrontCategoryService.replaceRels(id, request.getBackCategoryIds());
        return AjaxResult.success();
    }
}
