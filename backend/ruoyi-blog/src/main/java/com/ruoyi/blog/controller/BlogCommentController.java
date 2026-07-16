package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.CommentAuditRequest;
import com.ruoyi.blog.dto.CommentPageQuery;
import com.ruoyi.blog.service.BlogCommentService;
import com.ruoyi.blog.vo.CommentVO;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/comment")
@RequiredArgsConstructor
public class BlogCommentController extends BlogControllerSupport
{
    private final BlogCommentService blogCommentService;

    @PreAuthorize("@ss.hasPermi('blog:comment:list')")
    @GetMapping
    public TableDataInfo page(CommentPageQuery query)
    {
        Page<CommentVO> page = blogCommentService.adminPage(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:comment:audit')")
    @Log(title = "博客评论", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@Valid @RequestBody CommentAuditRequest request)
    {
        blogCommentService.audit(request);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('blog:comment:remove')")
    @Log(title = "博客评论", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id)
    {
        blogCommentService.deleteComment(id);
        return AjaxResult.success();
    }
}
