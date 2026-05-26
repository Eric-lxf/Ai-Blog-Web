package com.ruoyi.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.ArticlePageQuery;
import com.ruoyi.blog.dto.ArticleSaveRequest;
import com.ruoyi.blog.vo.ArticleBriefVO;
import com.ruoyi.blog.vo.ArticleVO;

public interface BlogArticleService
{

    Page<ArticleVO> page(ArticlePageQuery query);

    Page<ArticleBriefVO> publicPage(ArticlePageQuery query);

    ArticleVO getById(Long id);

    ArticleVO getPublishedById(Long id);

    Long save(ArticleSaveRequest request);

    void delete(Long id);
}
