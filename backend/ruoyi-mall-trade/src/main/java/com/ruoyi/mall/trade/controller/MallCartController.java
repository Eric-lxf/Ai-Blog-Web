package com.ruoyi.mall.trade.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.mall.trade.dto.CartAddRequest;
import com.ruoyi.mall.trade.dto.CartUpdateRequest;
import com.ruoyi.mall.trade.service.MallCartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/cart")
@RequiredArgsConstructor
public class MallCartController extends MallTradeControllerSupport
{
    private final MallCartService mallCartService;

    @GetMapping
    public AjaxResult list()
    {
        return AjaxResult.success(mallCartService.listMine());
    }

    @PostMapping
    public AjaxResult add(@Valid @RequestBody CartAddRequest request)
    {
        return AjaxResult.success(mallCartService.add(request));
    }

    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody CartUpdateRequest request)
    {
        mallCartService.update(id, request);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        mallCartService.delete(id);
        return AjaxResult.success();
    }

    @DeleteMapping
    public AjaxResult deleteBatch(@RequestBody List<Long> ids)
    {
        mallCartService.deleteBatch(ids);
        return AjaxResult.success();
    }
}
