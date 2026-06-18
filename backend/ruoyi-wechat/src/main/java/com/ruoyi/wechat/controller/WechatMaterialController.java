package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.wechat.dto.WechatMaterialBatchRequest;
import com.ruoyi.wechat.dto.WechatMaterialDeleteRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.service.WechatMaterialService;
import com.ruoyi.wechat.service.WechatMediaAssetService;
import com.ruoyi.wechat.vo.WechatMaterialVO;
import com.ruoyi.wechat.vo.WechatMediaAssetVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wechat/material")
public class WechatMaterialController extends WechatControllerSupport
{
    private final WechatMaterialService wechatMaterialService;
    private final WechatMediaAssetService wechatMediaAssetService;

    @PreAuthorize("@ss.hasPermi('wechat:material:list')")
    @GetMapping
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatMaterialVO> page = wechatMaterialService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:list')")
    @GetMapping("/assets")
    public TableDataInfo assetPage(@Valid WechatPageQuery query)
    {
        Page<WechatMediaAssetVO> page = wechatMediaAssetService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:add')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam Long accountId,
            @RequestParam String name,
            @RequestParam(defaultValue = "image") String mediaType,
            @RequestParam("file") MultipartFile file)
    {
        return AjaxResult.success(wechatMediaAssetService.upload(accountId, name, mediaType, file));
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:query')")
    @PostMapping("/wechat/batchget")
    public AjaxResult batchGetFromWechat(@Valid @RequestBody WechatMaterialBatchRequest request)
    {
        return AjaxResult.success(wechatMediaAssetService.batchGetFromWechat(request));
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:remove')")
    @PostMapping("/wechat/delete")
    public AjaxResult deleteFromWechat(@Valid @RequestBody WechatMaterialDeleteRequest request)
    {
        wechatMediaAssetService.deleteFromWechat(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:remove')")
    @DeleteMapping("/assets/{id}")
    public AjaxResult deleteAsset(@PathVariable Long id)
    {
        wechatMediaAssetService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:material:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        wechatMaterialService.delete(id);
        return AjaxResult.success();
    }
}
