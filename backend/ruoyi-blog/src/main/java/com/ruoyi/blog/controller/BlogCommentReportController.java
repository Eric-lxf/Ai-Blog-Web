package com.ruoyi.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.CommentReportPageQuery;
import com.ruoyi.blog.service.BlogCommentReportService;
import com.ruoyi.blog.vo.CommentReportVO;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog/comment/report")
@RequiredArgsConstructor
public class BlogCommentReportController extends BlogControllerSupport
{
    private final BlogCommentReportService reportService;

    @PreAuthorize("@ss.hasPermi('blog:comment:report:list')")
    @GetMapping
    public TableDataInfo page(CommentReportPageQuery query)
    {
        Page<CommentReportVO> page = reportService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:comment:report:handle')")
    @Log(title = "评论举报", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/handle")
    public AjaxResult handle(@PathVariable Long id, @Valid @RequestBody ReportHandleRequest request)
    {
        reportService.handle(id, request.getCommentStatus(), request.getHandleRemark());
        return AjaxResult.success();
    }

    @Data
    public static class ReportHandleRequest
    {
        private Integer commentStatus;
        private String handleRemark;
    }
}
