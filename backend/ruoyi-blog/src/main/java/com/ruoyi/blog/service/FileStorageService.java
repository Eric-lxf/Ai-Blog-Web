package com.ruoyi.blog.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService
{

    String storeImage(MultipartFile file);
}
