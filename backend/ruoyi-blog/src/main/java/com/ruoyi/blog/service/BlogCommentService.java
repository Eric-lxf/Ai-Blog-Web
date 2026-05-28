package com.ruoyi.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.CommentAuditRequest;
import com.ruoyi.blog.dto.CommentCreateRequest;
import com.ruoyi.blog.dto.CommentPageQuery;
import com.ruoyi.blog.dto.CommentReportRequest;
import com.ruoyi.blog.vo.CommentVO;

public interface BlogCommentService
{
    Page<CommentVO> publicPage(Long articleId, CommentPageQuery query, String guestKey);

    Long createComment(Long articleId, CommentCreateRequest request);

    Long replyComment(Long commentId, CommentCreateRequest request);

    boolean toggleLike(Long commentId, String guestKey);

    void reportComment(Long commentId, CommentReportRequest request, String guestKey);

    Page<CommentVO> adminPage(CommentPageQuery query);

    void audit(CommentAuditRequest request);

    void deleteComment(Long id);
}
