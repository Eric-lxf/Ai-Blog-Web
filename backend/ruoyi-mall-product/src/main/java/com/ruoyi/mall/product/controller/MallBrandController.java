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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.mall.product.domain.MallBrand;
import com.ruoyi.mall.product.dto.MallBrandPageQuery;
import com.ruoyi.mall.product.dto.MallBrandSaveRequest;
import com.ruoyi.mall.product.service.MallBrandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/brand")
@RequiredArgsConstructor
public class MallBrandController extends MallProductControllerSupport
{
    private final MallBrandService mallBrandService;

    @PreAuthorize("@ss.hasPermi('mall:brand:list')")
    @GetMapping
    public TableDataInfo page(@Valid MallBrandPageQuery query)
    {
        Page<MallBrand> page = mallBrandService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('mall:brand:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallBrandService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:brand:add')")
    @Log(title = "商城品牌", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody MallBrandSaveRequest request)
    {
        return AjaxResult.success(mallBrandService.create(request));
    }

    @PreAuthorize("@ss.hasPermi('mall:brand:edit')")
    @Log(title = "商城品牌", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody MallBrandSaveRequest request)
    {
        mallBrandService.update(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:brand:remove')")
    @Log(title = "商城品牌", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        mallBrandService.delete(id);
        return AjaxResult.success();
    }
}
