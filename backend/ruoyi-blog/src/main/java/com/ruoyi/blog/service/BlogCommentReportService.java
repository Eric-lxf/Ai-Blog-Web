package com.ruoyi.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogCommentReport;
import com.ruoyi.blog.dto.CommentReportPageQuery;
import com.ruoyi.blog.vo.CommentReportVO;

public interface BlogCommentReportService
{
    Page<CommentReportVO> page(CommentReportPageQuery query);

    void handle(Long id, Integer commentStatus, String handleRemark);
}
