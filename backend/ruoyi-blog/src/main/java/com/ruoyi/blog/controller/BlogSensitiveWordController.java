package com.ruoyi.blog.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.blog.domain.BlogSensitiveWord;
import com.ruoyi.blog.service.BlogSensitiveWordService;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/sensitive-word")
@RequiredArgsConstructor
public class BlogSensitiveWordController
{
    private final BlogSensitiveWordService sensitiveWordService;

    @PreAuthorize("@ss.hasPermi('blog:sensitive:list')")
    @GetMapping
    public AjaxResult list(BlogSensitiveWord query)
    {
        List<BlogSensitiveWord> list = sensitiveWordService.list(query);
        return AjaxResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('blog:sensitive:list')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id)
    {
        return AjaxResult.success(sensitiveWordService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('blog:sensitive:add')")
    @Log(title = "敏感词", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BlogSensitiveWord word)
    {
        return AjaxResult.success(sensitiveWordService.insert(word));
    }

    @PreAuthorize("@ss.hasPermi('blog:sensitive:edit')")
    @Log(title = "敏感词", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BlogSensitiveWord word)
    {
        return AjaxResult.success(sensitiveWordService.update(word));
    }

    @PreAuthorize("@ss.hasPermi('blog:sensitive:remove')")
    @Log(title = "敏感词", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return AjaxResult.success(sensitiveWordService.deleteByIds(ids));
    }
}
