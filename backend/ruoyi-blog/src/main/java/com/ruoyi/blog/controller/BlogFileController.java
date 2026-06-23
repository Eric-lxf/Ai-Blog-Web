package com.ruoyi.blog.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogFile;
import com.ruoyi.blog.service.BlogFileService;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/file")
@RequiredArgsConstructor
public class BlogFileController extends BlogControllerSupport
{

    private final BlogFileService blogFileService;

    @PreAuthorize("@ss.hasPermi('blog:file:upload')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file)
    {
        BlogFile record = blogFileService.upload(file);
        return AjaxResult.success(record);
    }

    @PreAuthorize("@ss.hasPermi('blog:file:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(defaultValue = "1")  int    pageNum,
            @RequestParam(defaultValue = "20") int    pageSize,
            @RequestParam(defaultValue = "")   String keyword)
    {
        Page<BlogFile> page = blogFileService.page(pageNum, pageSize, keyword);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:file:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        blogFileService.delete(id);
        return AjaxResult.success();
    }
}