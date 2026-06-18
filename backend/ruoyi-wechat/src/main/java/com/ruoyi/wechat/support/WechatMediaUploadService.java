package com.ruoyi.wechat.support;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.constant.WechatConstants;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WechatMediaUploadService
{
    private final WechatApiClient wechatApiClient;
    private final WechatTokenService wechatTokenService;
    private final WechatImageResolver wechatImageResolver;

    /**
     * 上传封面图，返回永久素材 thumb_media_id。
     */
    public String uploadThumb(Long accountId, String imageUrl)
    {
        byte[] bytes = wechatImageResolver.readImageBytes(imageUrl);
        String filename = wechatImageResolver.filenameOf(imageUrl);
        String contentType = wechatImageResolver.contentTypeOf(filename);
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/material/add_material?access_token=" + token + "&type=thumb";
        Map<String, Object> resp = wechatApiClient.postMultipart(url, "media", filename, bytes, contentType);
        WechatApiErrors.assertOk(resp, "upload thumb image");
        Object mediaId = resp.get("media_id");
        if (mediaId == null)
        {
            throw new ServiceException("upload thumb image failed: media_id missing", HttpStatus.ERROR);
        }
        return String.valueOf(mediaId);
    }

    /**
     * 上传正文图片，返回微信 CDN URL。
     */
    public String uploadContentImage(Long accountId, String imageUrl)
    {
        byte[] bytes = wechatImageResolver.readImageBytes(imageUrl);
        String filename = wechatImageResolver.filenameOf(imageUrl);
        String contentType = wechatImageResolver.contentTypeOf(filename);
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/media/uploadimg?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postMultipart(url, "media", filename, bytes, contentType);
        WechatApiErrors.assertOk(resp, "upload content image");
        Object cdnUrl = resp.get("url");
        if (cdnUrl == null)
        {
            throw new ServiceException("upload content image failed: url missing", HttpStatus.ERROR);
        }
        return String.valueOf(cdnUrl);
    }
}
