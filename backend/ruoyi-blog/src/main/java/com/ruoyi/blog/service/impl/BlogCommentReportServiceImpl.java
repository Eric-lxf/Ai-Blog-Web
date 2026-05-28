package com.ruoyi.blog.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.constant.BlogCommentConstants;
import com.ruoyi.blog.domain.BlogArticle;
import com.ruoyi.blog.domain.BlogComment;
import com.ruoyi.blog.domain.BlogCommentReport;
import com.ruoyi.blog.dto.CommentReportPageQuery;
import com.ruoyi.blog.mapper.BlogArticleMapper;
import com.ruoyi.blog.mapper.BlogCommentMapper;
import com.ruoyi.blog.mapper.BlogCommentReportMapper;
import com.ruoyi.blog.service.BlogCommentReportService;
import com.ruoyi.blog.vo.CommentReportVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogCommentReportServiceImpl implements BlogCommentReportService
{
    private final BlogCommentReportMapper reportMapper;
    private final BlogCommentMapper commentMapper;
    private final BlogArticleMapper articleMapper;

    @Override
    public Page<CommentReportVO> page(CommentReportPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<BlogCommentReport> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null)
        {
            wrapper.eq(BlogCommentReport::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(BlogCommentReport::getCreateTime);
        Page<BlogCommentReport> page = reportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<CommentReportVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void handle(Long id, Integer commentStatus, String handleRemark)
    {
        BlogCommentReport report = reportMapper.selectById(id);
        if (report == null)
        {
            throw new ServiceException("举报记录不存在", HttpStatus.NOT_FOUND);
        }
        BlogComment comment = commentMapper.selectById(report.getCommentId());
        if (comment != null && commentStatus != null)
        {
            comment.setStatus(commentStatus);
            commentMapper.updateById(comment);
        }
        report.setStatus(BlogCommentConstants.REPORT_HANDLED);
        report.setHandleRemark(handleRemark);
        report.setHandleBy(SecurityUtils.getUsername());
        report.setHandleTime(java.time.LocalDateTime.now());
        reportMapper.updateById(report);
    }

    private CommentReportVO toVo(BlogCommentReport report)
    {
        CommentReportVO vo = new CommentReportVO();
        vo.setId(report.getId());
        vo.setCommentId(report.getCommentId());
        vo.setReason(report.getReason());
        vo.setStatus(report.getStatus());
        vo.setHandleRemark(report.getHandleRemark());
        vo.setHandleBy(report.getHandleBy());
        vo.setHandleTime(report.getHandleTime());
        vo.setCreateTime(report.getCreateTime());
        BlogComment comment = commentMapper.selectById(report.getCommentId());
        if (comment != null)
        {
            vo.setCommentContent(comment.getContent());
            BlogArticle article = articleMapper.selectById(comment.getArticleId());
            if (article != null)
            {
                vo.setArticleTitle(article.getTitle());
            }
        }
        return vo;
    }
}
