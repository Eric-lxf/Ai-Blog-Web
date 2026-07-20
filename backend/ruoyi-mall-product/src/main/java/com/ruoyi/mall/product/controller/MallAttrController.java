package com.ruoyi.mall.product.controller;

import java.util.List;

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
import com.ruoyi.mall.product.domain.MallAttr;
import com.ruoyi.mall.product.dto.MallAttrOptionSaveRequest;
import com.ruoyi.mall.product.dto.MallAttrPageQuery;
import com.ruoyi.mall.product.dto.MallAttrSaveRequest;
import com.ruoyi.mall.product.service.MallAttrService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/attr")
@RequiredArgsConstructor
public class MallAttrController extends MallProductControllerSupport
{
    private final MallAttrService mallAttrService;

    @PreAuthorize("@ss.hasPermi('mall:attr:list')")
    @GetMapping
    public TableDataInfo page(@Valid MallAttrPageQuery query)
    {
        Page<MallAttr> page = mallAttrService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('mall:attr:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallAttrService.get(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:attr:add')")
    @Log(title = "商城属性", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody MallAttrSaveRequest request)
    {
        return AjaxResult.success(mallAttrService.create(request));
    }

    @PreAuthorize("@ss.hasPermi('mall:attr:edit')")
    @Log(title = "商城属性", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody MallAttrSaveRequest request)
    {
        mallAttrService.update(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:attr:remove')")
    @Log(title = "商城属性", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        mallAttrService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:attr:query')")
    @GetMapping("/{id}/options")
    public AjaxResult listOptions(@PathVariable Long id)
    {
        return AjaxResult.success(mallAttrService.listOptions(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:attr:edit')")
    @Log(title = "商城属性选项", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/options")
    public AjaxResult replaceOptions(@PathVariable Long id,
            @Valid @RequestBody List<MallAttrOptionSaveRequest> options)
    {
        mallAttrService.replaceOptions(id, options);
        return AjaxResult.success();
    }
}
