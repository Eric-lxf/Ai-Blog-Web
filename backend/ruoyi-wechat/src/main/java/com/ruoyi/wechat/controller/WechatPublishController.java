package com.ruoyi.wechat.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.wechat.dto.WechatFreePublishArticleRequest;
import com.ruoyi.wechat.dto.WechatFreePublishBatchRequest;
import com.ruoyi.wechat.dto.WechatFreePublishDeleteRequest;
import com.ruoyi.wechat.dto.WechatFreePublishStatusRequest;
import com.ruoyi.wechat.dto.WechatFreePublishSubmitRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatPushRequest;
import com.ruoyi.wechat.service.WechatFreePublishService;
import com.ruoyi.wechat.service.WechatPublishService;
import com.ruoyi.wechat.service.WechatPushOrchestrator;
import com.ruoyi.wechat.vo.WechatPublishRecordVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WechatPublishController extends WechatControllerSupport
{
    private final WechatPublishService wechatPublishService;
    private final WechatPushOrchestrator wechatPushOrchestrator;
    private final WechatFreePublishService wechatFreePublishService;

    @PreAuthorize("@ss.hasPermi('wechat:publish:list')")
    @GetMapping("/wechat/publish")
    public TableDataInfo page(@Valid WechatPageQuery query)
    {
        Page<WechatPublishRecordVO> page = wechatPublishService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:push')")
    @Log(title = "微信发布", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/wechat/push")
    public AjaxResult push(@Valid @RequestBody WechatPushRequest request)
    {
        Long recordId = wechatPushOrchestrator.push(request);
        return AjaxResult.success(recordId);
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:push')")
    @Log(title = "微信发布", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/wechat/publish/push")
    public AjaxResult pushViaPublish(@Valid @RequestBody WechatPushRequest request)
    {
        Long recordId = wechatPushOrchestrator.push(request);
        return AjaxResult.success(recordId);
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:push')")
    @Log(title = "微信发布", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/wechat/publish/{id}/submit")
    public AjaxResult submitFromRecord(@PathVariable Long id)
    {
        return AjaxResult.success(wechatPublishService.submitFromRecord(id));
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:query')")
    @Log(title = "微信发布", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/wechat/publish/{id}/sync-status")
    public AjaxResult syncStatus(@PathVariable Long id)
    {
        return AjaxResult.success(wechatPublishService.syncStatus(id));
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:list')")
    @Log(title = "微信发布", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/wechat/publish/wechat/batchget")
    public AjaxResult batchGet(@Valid @RequestBody WechatFreePublishBatchRequest request)
    {
        return AjaxResult.success(wechatFreePublishService.batchGet(
                request.getAccountId(),
                request.getOffset(),
                request.getCount(),
                request.getNoContent()));
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:remove')")
    @Log(title = "微信发布", businessType = BusinessType.DELETE)
    @PostMapping("/wechat/publish/wechat/delete")
    public AjaxResult deletePublished(@Valid @RequestBody WechatFreePublishDeleteRequest request)
    {
        wechatFreePublishService.delete(request.getAccountId(), request.getArticleId(), request.getIndex());
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:query')")
    @Log(title = "微信发布", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/wechat/publish/wechat/status")
    public AjaxResult getPublishStatus(@Valid @RequestBody WechatFreePublishStatusRequest request)
    {
        return AjaxResult.success(wechatFreePublishService.getStatus(request.getAccountId(), request.getPublishId()));
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:list')")
    @Log(title = "微信发布", businessType = BusinessType.SYNC, isSaveResponseData = false)
    @PostMapping("/wechat/publish/wechat/article")
    public AjaxResult getPublishedArticle(@Valid @RequestBody WechatFreePublishArticleRequest request)
    {
        return AjaxResult.success(wechatFreePublishService.getArticle(request.getAccountId(), request.getArticleId()));
    }

    @PreAuthorize("@ss.hasPermi('wechat:publish:push')")
    @Log(title = "微信发布", businessType = BusinessType.PUBLISH, isSaveResponseData = false)
    @PostMapping("/wechat/publish/wechat/submit")
    public AjaxResult submitDraft(@Valid @RequestBody WechatFreePublishSubmitRequest request)
    {
        return AjaxResult.success(wechatFreePublishService.submit(request.getAccountId(), request.getMediaId()));
    }
}
