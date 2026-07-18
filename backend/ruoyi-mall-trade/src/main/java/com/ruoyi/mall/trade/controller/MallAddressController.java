package com.ruoyi.mall.trade.controller;

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
import com.ruoyi.mall.trade.dto.AddressSaveRequest;
import com.ruoyi.mall.trade.service.MallAddressService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mall/address")
@RequiredArgsConstructor
public class MallAddressController extends MallTradeControllerSupport
{
    private final MallAddressService mallAddressService;

    @GetMapping
    public AjaxResult list()
    {
        return AjaxResult.success(mallAddressService.listMine());
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(mallAddressService.getMine(id));
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody AddressSaveRequest request)
    {
        return AjaxResult.success(mallAddressService.create(request));
    }

    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody AddressSaveRequest request)
    {
        mallAddressService.update(id, request);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        mallAddressService.delete(id);
        return AjaxResult.success();
    }
}
