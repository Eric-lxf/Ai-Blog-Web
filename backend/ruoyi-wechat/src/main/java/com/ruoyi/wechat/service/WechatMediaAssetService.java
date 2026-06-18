package com.ruoyi.wechat.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatMaterialBatchRequest;
import com.ruoyi.wechat.dto.WechatMaterialDeleteRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatMediaAssetVO;

public interface WechatMediaAssetService
{
    Page<WechatMediaAssetVO> page(WechatPageQuery query);

    Long upload(Long accountId, String name, String mediaType, MultipartFile file);

    void delete(Long id);

    Map<String, Object> batchGetFromWechat(WechatMaterialBatchRequest request);

    void deleteFromWechat(WechatMaterialDeleteRequest request);
}
