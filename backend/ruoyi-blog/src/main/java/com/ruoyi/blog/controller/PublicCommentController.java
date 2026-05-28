package com.ruoyi.blog.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.CommentCreateRequest;
import com.ruoyi.blog.dto.CommentPageQuery;
import com.ruoyi.blog.dto.CommentReportRequest;
import com.ruoyi.blog.service.BlogCommentService;
import com.ruoyi.blog.util.BlogCommentUtils;
import com.ruoyi.blog.vo.CommentVO;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;

import lombok.RequiredArgsConstructor;

@Anonymous
@RestController
@RequestMapping("/public/blog")
@RequiredArgsConstructor
public class PublicCommentController extends BlogControllerSupport
{
    private final BlogCommentService blogCommentService;

    @GetMapping("/articles/{articleId}/comments")
    public TableDataInfo list(@PathVariable Long articleId, CommentPageQuery query)
    {
        String guestKey = BlogCommentUtils.currentGuestKey();
        Page<CommentVO> page = blogCommentService.publicPage(articleId, query, guestKey);
        return mpPageTable(page);
    }

    @PostMapping("/articles/{articleId}/comments")
    public AjaxResult create(@PathVariable Long articleId, @Valid @RequestBody CommentCreateRequest request)
    {
        Long id = blogCommentService.createComment(articleId, request);
        return AjaxResult.success("评论已提交，审核通过后展示", id);
    }

    @PostMapping("/comments/{commentId}/reply")
    public AjaxResult reply(@PathVariable Long commentId, @Valid @RequestBody CommentCreateRequest request)
    {
        Long id = blogCommentService.replyComment(commentId, request);
        return AjaxResult.success("回复已提交，审核通过后展示", id);
    }

    @PostMapping("/comments/{commentId}/like")
    public AjaxResult like(@PathVariable Long commentId)
    {
        String guestKey = BlogCommentUtils.currentGuestKey();
        boolean liked = blogCommentService.toggleLike(commentId, guestKey);
        return AjaxResult.success(Map.of("liked", liked));
    }

    @PostMapping("/comments/{commentId}/report")
    public AjaxResult report(@PathVariable Long commentId, @Valid @RequestBody CommentReportRequest request)
    {
        String guestKey = BlogCommentUtils.currentGuestKey();
        blogCommentService.reportComment(commentId, request, guestKey);
        return AjaxResult.success("举报已提交");
    }
}
