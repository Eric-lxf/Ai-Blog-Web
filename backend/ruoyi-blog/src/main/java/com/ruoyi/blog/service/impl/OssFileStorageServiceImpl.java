package com.ruoyi.blog.service.impl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.ruoyi.blog.config.OssProperties;
import com.ruoyi.blog.service.FileStorageService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "novamall.oss", name = "enabled", havingValue = "true")
public class OssFileStorageServiceImpl implements FileStorageService
{

    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final OssProperties ossProperties;

    @Override
    public String storeImage(MultipartFile file)
    {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_IMAGE_EXT.contains(ext.toLowerCase()))
        {
            throw new ServiceException("仅支持 jpg/png/gif/webp 图片", HttpStatus.BAD_REQUEST);
        }
        return upload(file, "images");
    }

    @Override
    public String storeFile(MultipartFile file)
    {
        return upload(file, "files");
    }

    private String upload(MultipartFile file, String subDir)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空", HttpStatus.BAD_REQUEST);
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID().toString().replace("-", "")
                + (ext != null ? "." + ext.toLowerCase() : "");
        String prefix = ossProperties.getObjectPrefix();
        if (!prefix.endsWith("/")) prefix = prefix + "/";
        String objectKey = prefix + subDir + "/" + dateDir + "/" + filename;

        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret());
        try (InputStream in = file.getInputStream())
        {
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            if (StringUtils.hasText(file.getContentType()))
            {
                meta.setContentType(file.getContentType());
            }
            ossClient.putObject(ossProperties.getBucketName(), objectKey, in, meta);
            return buildUrl(objectKey);
        }
        catch (Exception e)
        {
            log.error("OSS 上传失败: key={}", objectKey, e);
            throw new ServiceException("文件上传失败: " + e.getMessage(), HttpStatus.ERROR);
        }
        finally
        {
            ossClient.shutdown();
        }
    }

    private String buildUrl(String objectKey)
    {
        String domain = ossProperties.getCustomDomain();
        if (StringUtils.hasText(domain))
        {
            if (!domain.startsWith("http"))
            {
                domain = "https://" + domain;
            }
            return domain.replaceAll("/+$", "") + "/" + objectKey;
        }
        String endpoint = ossProperties.getEndpoint();
        String bucket   = ossProperties.getBucketName();
        if (!endpoint.startsWith("http"))
        {
            endpoint = "https://" + endpoint;
        }
        return endpoint.replace("://", "://" + bucket + ".") + "/" + objectKey;
    }
}