package com.ruoyi.wechat.support;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ruoyi.blog.config.BlogFileProperties;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@RequiredArgsConstructor
public class WechatImageResolver
{
    private final BlogFileProperties blogFileProperties;

    public byte[] readImageBytes(String imageUrl)
    {
        if (!StringUtils.hasText(imageUrl))
        {
            throw new ServiceException("image url is empty", HttpStatus.BAD_REQUEST);
        }
        String trimmed = imageUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
        {
            return downloadRemote(trimmed);
        }
        Path localPath = resolveLocalPath(trimmed);
        if (!Files.exists(localPath))
        {
            throw new ServiceException("image file not found: " + trimmed, HttpStatus.BAD_REQUEST);
        }
        try
        {
            return Files.readAllBytes(localPath);
        }
        catch (IOException e)
        {
            throw new ServiceException("read image failed: " + e.getMessage(), HttpStatus.ERROR);
        }
    }

    public String filenameOf(String imageUrl)
    {
        String path = imageUrl;
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))
        {
            path = URI.create(imageUrl).getPath();
        }
        String name = Paths.get(path).getFileName().toString();
        return StringUtils.hasText(name) ? name : "image.jpg";
    }

    public String contentTypeOf(String filename)
    {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png"))
        {
            return "image/png";
        }
        if (lower.endsWith(".gif"))
        {
            return "image/gif";
        }
        if (lower.endsWith(".webp"))
        {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private Path resolveLocalPath(String imageUrl)
    {
        String prefix = blogFileProperties.getUrlPrefix();
        if (!prefix.startsWith("/"))
        {
            prefix = "/" + prefix;
        }
        String relative = imageUrl;
        if (relative.startsWith(prefix))
        {
            relative = relative.substring(prefix.length());
        }
        if (relative.startsWith("/"))
        {
            relative = relative.substring(1);
        }
        return Paths.get(blogFileProperties.getUploadDir(), relative);
    }

    private byte[] downloadRemote(String url)
    {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful() || response.body() == null)
            {
                throw new ServiceException("download image failed: " + response.code(), HttpStatus.ERROR);
            }
            return response.body().bytes();
        }
        catch (IOException e)
        {
            throw new ServiceException("download image failed: " + e.getMessage(), HttpStatus.ERROR);
        }
    }
}
