package com.ruoyi.wechat.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    public Map<String, String> uploadPermanentMaterial(Long accountId, byte[] bytes, String filename, String type)
    {
        if (bytes == null || bytes.length == 0)
        {
            throw new ServiceException("upload file is empty", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(type))
        {
            throw new ServiceException("material type is required", HttpStatus.BAD_REQUEST);
        }
        String contentType = wechatImageResolver.contentTypeOf(filename);
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/material/add_material?access_token=" + token + "&type=" + type;
        Map<String, Object> resp = wechatApiClient.postMultipart(url, "media", filename, bytes, contentType);
        WechatApiErrors.assertOk(resp, "upload permanent material");
        Map<String, String> result = new LinkedHashMap<>();
        if (resp.get("media_id") != null)
        {
            result.put("media_id", String.valueOf(resp.get("media_id")));
        }
        if (resp.get("url") != null)
        {
            result.put("url", String.valueOf(resp.get("url")));
        }
        return result;
    }

    /**
     * 上传封面图，返回永久素材 thumb_media_id。
     */
    public String uploadThumb(Long accountId, String imageUrl)
    {
        byte[] bytes = wechatImageResolver.readImageBytes(imageUrl);
        String filename = wechatImageResolver.filenameOf(imageUrl);
        Map<String, String> result = uploadPermanentMaterial(accountId, bytes, filename, "thumb");
        String mediaId = result.get("media_id");
        if (!StringUtils.hasText(mediaId))
        {
            throw new ServiceException("upload thumb image failed: media_id missing", HttpStatus.ERROR);
        }
        return mediaId;
    }

    /**
     * 上传正文图片，返回微信 CDN URL。
     */
    public String uploadContentImage(Long accountId, String imageUrl)
    {
        byte[] bytes = wechatImageResolver.readImageBytes(imageUrl);
        String filename = wechatImageResolver.filenameOf(imageUrl);
        return uploadContentImageBytes(accountId, bytes, filename);
    }

    public String uploadContentImageBytes(Long accountId, byte[] bytes, String filename)
    {
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

    public void deletePermanentMaterial(Long accountId, String mediaId)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/material/del_material?access_token=" + token;
        Map<String, Object> resp = wechatApiClient.postJson(url, Map.of("media_id", mediaId));
        WechatApiErrors.assertOk(resp, "delete permanent material");
    }

    public Map<String, Object> batchGetMaterials(Long accountId, String type, int offset, int count)
    {
        String token = wechatTokenService.getAccessToken(accountId);
        String url = WechatConstants.API_HOST + "/cgi-bin/material/batchget_material?access_token=" + token;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("offset", Math.max(offset, 0));
        payload.put("count", Math.min(Math.max(count, 1), 20));
        Map<String, Object> resp = wechatApiClient.postJson(url, payload);
        WechatApiErrors.assertOk(resp, "batchget material");
        return resp;
    }
}
