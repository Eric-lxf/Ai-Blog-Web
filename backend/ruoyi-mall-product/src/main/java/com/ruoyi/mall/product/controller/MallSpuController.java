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
import com.ruoyi.mall.product.dto.MallSpuPageQuery;
import com.ruoyi.mall.product.dto.MallSpuPublishRequest;
import com.ruoyi.mall.product.dto.MallSpuSaveRequest;
import com.ruoyi.mall.product.service.MallSpuService;
import com.ruoyi.mall.product.vo.MallSpuVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/spu")
@RequiredArgsConstructor
public class MallSpuController extends MallProductControllerSupport
{
    private final MallSpuService mallSpuService;

    @PreAuthorize("@ss.hasPermi('mall:spu:list')")
    @GetMapping
    public TableDataInfo page(@Valid MallSpuPageQuery query)
    {
        Page<MallSpuVO> page = mallSpuService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('mall:spu:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallSpuService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('mall:spu:add') or @ss.hasPermi('mall:spu:edit')")
    @Log(title = "商城商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@Valid @RequestBody MallSpuSaveRequest request)
    {
        return AjaxResult.success(mallSpuService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('mall:spu:remove')")
    @Log(title = "商城商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        mallSpuService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('mall:spu:publish')")
    @Log(title = "商城商品上下架", businessType = BusinessType.PUBLISH)
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id, @Valid @RequestBody MallSpuPublishRequest request)
    {
        mallSpuService.publish(id, request.getStatus());
        return AjaxResult.success();
    }
}
