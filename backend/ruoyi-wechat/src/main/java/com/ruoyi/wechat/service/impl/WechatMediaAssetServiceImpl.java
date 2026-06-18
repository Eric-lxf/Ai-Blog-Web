package com.ruoyi.wechat.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.domain.WechatMediaAsset;
import com.ruoyi.wechat.dto.WechatMaterialBatchRequest;
import com.ruoyi.wechat.dto.WechatMaterialDeleteRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.mapper.WechatMediaAssetMapper;
import com.ruoyi.wechat.service.WechatMediaAssetService;
import com.ruoyi.wechat.support.WechatMediaUploadService;
import com.ruoyi.wechat.vo.WechatMediaAssetVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatMediaAssetServiceImpl implements WechatMediaAssetService
{
    private final WechatMediaAssetMapper wechatMediaAssetMapper;
    private final WechatMediaUploadService wechatMediaUploadService;

    @Override
    public Page<WechatMediaAssetVO> page(WechatPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<WechatMediaAsset> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatMediaAsset> wrapper = new LambdaQueryWrapper<>();
        if (query.getAccountId() != null)
        {
            wrapper.eq(WechatMediaAsset::getAccountId, query.getAccountId());
        }
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.like(WechatMediaAsset::getName, query.getKeyword().trim());
        }
        wrapper.orderByDesc(WechatMediaAsset::getUpdateTime);
        Page<WechatMediaAsset> result = wechatMediaAssetMapper.selectPage(page, wrapper);
        Page<WechatMediaAssetVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional
    public Long upload(Long accountId, String name, String mediaType, MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("upload file is empty", HttpStatus.BAD_REQUEST);
        }
        String type = normalizeMediaType(mediaType);
        String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.jpg";
        byte[] bytes = readBytes(file);
        Map<String, String> uploadResult;
        if ("content".equals(type))
        {
            String cdnUrl = wechatMediaUploadService.uploadContentImageBytes(accountId, bytes, filename);
            uploadResult = Map.of("url", cdnUrl);
        }
        else
        {
            String wechatType = "thumb".equals(type) ? "thumb" : "image";
            uploadResult = wechatMediaUploadService.uploadPermanentMaterial(accountId, bytes, filename, wechatType);
        }
        WechatMediaAsset asset = new WechatMediaAsset();
        asset.setAccountId(accountId);
        asset.setName(StringUtils.hasText(name) ? name.trim() : filename);
        asset.setMediaType(type);
        asset.setMediaId(uploadResult.get("media_id"));
        asset.setUrl(uploadResult.get("url"));
        asset.setFileName(filename);
        wechatMediaAssetMapper.insert(asset);
        return asset.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        WechatMediaAsset asset = wechatMediaAssetMapper.selectById(id);
        if (asset == null)
        {
            throw new ServiceException("media asset not found", HttpStatus.NOT_FOUND);
        }
        if (StringUtils.hasText(asset.getMediaId()) && !"content".equals(asset.getMediaType()))
        {
            wechatMediaUploadService.deletePermanentMaterial(asset.getAccountId(), asset.getMediaId());
        }
        wechatMediaAssetMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> batchGetFromWechat(WechatMaterialBatchRequest request)
    {
        return wechatMediaUploadService.batchGetMaterials(
                request.getAccountId(),
                request.getType(),
                request.getOffset(),
                request.getCount());
    }

    @Override
    public void deleteFromWechat(WechatMaterialDeleteRequest request)
    {
        wechatMediaUploadService.deletePermanentMaterial(request.getAccountId(), request.getMediaId());
    }

    private String normalizeMediaType(String mediaType)
    {
        if (!StringUtils.hasText(mediaType))
        {
            return "image";
        }
        String type = mediaType.trim().toLowerCase();
        if ("thumb".equals(type) || "image".equals(type) || "content".equals(type))
        {
            return type;
        }
        throw new ServiceException("mediaType must be image, thumb or content", HttpStatus.BAD_REQUEST);
    }

    private byte[] readBytes(MultipartFile file)
    {
        try
        {
            return file.getBytes();
        }
        catch (IOException e)
        {
            throw new ServiceException("read upload file failed", HttpStatus.BAD_REQUEST);
        }
    }

    private WechatMediaAssetVO toVO(WechatMediaAsset asset)
    {
        WechatMediaAssetVO vo = new WechatMediaAssetVO();
        BeanUtils.copyProperties(asset, vo);
        return vo;
    }
}
