package com.ruoyi.blog.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.blog.service.FileStorageService;
import com.ruoyi.common.core.domain.AjaxResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/upload")
@RequiredArgsConstructor
public class FileUploadController extends BlogControllerSupport
{

    private final FileStorageService fileStorageService;

    @PreAuthorize("@ss.hasPermi('blog:upload:image')")
    @PostMapping("/image")
    public AjaxResult uploadImage(@RequestParam("file") MultipartFile file)
    {
        String url = fileStorageService.storeImage(file);
        return AjaxResult.success(Map.of("url", url));
    }
}
