package com.ruoyi.blog.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService
{

    /** 仅存储图片，返回访问 URL（供文章编辑器使用）。 */
    String storeImage(MultipartFile file);

    /** 存储任意类型文件，返回访问 URL。 */
    String storeFile(MultipartFile file);
}