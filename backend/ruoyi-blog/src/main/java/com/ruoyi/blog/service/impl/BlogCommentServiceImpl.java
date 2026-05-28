package com.ruoyi.blog.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.constant.BlogCommentConstants;
import com.ruoyi.blog.domain.BlogArticle;
import com.ruoyi.blog.domain.BlogComment;
import com.ruoyi.blog.domain.BlogCommentLike;
import com.ruoyi.blog.domain.BlogCommentReport;
import com.ruoyi.blog.dto.CommentAuditRequest;
import com.ruoyi.blog.dto.CommentCreateRequest;
import com.ruoyi.blog.dto.CommentPageQuery;
import com.ruoyi.blog.dto.CommentReportRequest;
import com.ruoyi.blog.mapper.BlogArticleMapper;
import com.ruoyi.blog.mapper.BlogCommentLikeMapper;
import com.ruoyi.blog.mapper.BlogCommentMapper;
import com.ruoyi.blog.mapper.BlogCommentReportMapper;
import com.ruoyi.blog.service.AiCommentModerationService;
import com.ruoyi.blog.service.BlogCommentService;
import com.ruoyi.blog.service.CommentConfigService;
import com.ruoyi.blog.service.impl.SensitiveWordFilter.FilterResult;
import com.ruoyi.blog.util.BlogCommentUtils;
import com.ruoyi.blog.vo.CommentVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogCommentServiceImpl implements BlogCommentService
{
    private static final int STATUS_PUBLISHED = 1;

    private final BlogCommentMapper commentMapper;
    private final BlogCommentLikeMapper likeMapper;
    private final BlogCommentReportMapper reportMapper;
    private final BlogArticleMapper articleMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final CommentConfigService commentConfigService;
    private final RedisCache redisCache;
    private final ISysUserService sysUserService;
    private final AiCommentModerationService aiCommentModerationService;

    @Override
    public Page<CommentVO> publicPage(Long articleId, CommentPageQuery query, String guestKey)
    {
        requirePublishedArticle(articleId);
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 50);
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogComment::getArticleId, articleId)
                .eq(BlogComment::getStatus, BlogCommentConstants.STATUS_APPROVED)
                .isNull(BlogComment::getParentId);
        applySort(wrapper, query.getSort());
        Page<BlogComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        LoginUser loginUser = BlogCommentUtils.loginUserOrNull();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        List<CommentVO> tree = buildTree(page.getRecords(), userId, guestKey);
        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(tree);
        return voPage;
    }

    @Override
    @Transactional
    public Long createComment(Long articleId, CommentCreateRequest request)
    {
        requirePublishedArticle(articleId);
        LoginUser loginUser = BlogCommentUtils.loginUserOrNull();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        checkRateLimit(userId);
        BlogComment comment = buildComment(articleId, null, request, loginUser);
        commentMapper.insert(comment);
        if (comment.getParentId() == null)
        {
            comment.setRootId(comment.getId());
            commentMapper.updateById(comment);
        }
        refreshSortScore(comment.getId());
        aiCommentModerationService.reviewAsync(comment.getId());
        return comment.getId();
    }

    @Override
    @Transactional
    public Long replyComment(Long commentId, CommentCreateRequest request)
    {
        BlogComment parent = requireComment(commentId);
        requirePublishedArticle(parent.getArticleId());
        LoginUser loginUser = BlogCommentUtils.loginUserOrNull();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        checkRateLimit(userId);
        BlogComment comment = buildComment(parent.getArticleId(), parent, request, loginUser);
        commentMapper.insert(comment);
        incrementReplyCount(parent.getId());
        if (parent.getRootId() != null && !parent.getRootId().equals(parent.getId()))
        {
            incrementReplyCount(parent.getRootId());
        }
        refreshSortScore(comment.getId());
        aiCommentModerationService.reviewAsync(comment.getId());
        return comment.getId();
    }

    @Override
    @Transactional
    public boolean toggleLike(Long commentId, String guestKey)
    {
        BlogComment comment = requireComment(commentId);
        if (comment.getStatus() == null || comment.getStatus() != BlogCommentConstants.STATUS_APPROVED)
        {
            throw new ServiceException("只能为已通过评论点赞");
        }
        LoginUser loginUser = BlogCommentUtils.loginUserOrNull();
        Long userId = loginUser == null ? null : loginUser.getUserId();
        if (userId == null && !StringUtils.hasText(guestKey))
        {
            throw new ServiceException("无法识别点赞用户");
        }
        Long likeId = likeMapper.findLikeId(commentId, userId, guestKey);
        if (likeId != null)
        {
            likeMapper.deleteById(likeId);
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() == null ? 0 : comment.getLikeCount()) - 1));
            commentMapper.updateById(comment);
            refreshSortScore(commentId);
            return false;
        }
        BlogCommentLike like = new BlogCommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setGuestKey(userId == null ? guestKey : null);
        likeMapper.insert(like);
        comment.setLikeCount((comment.getLikeCount() == null ? 0 : comment.getLikeCount()) + 1);
        commentMapper.updateById(comment);
        refreshSortScore(commentId);
        return true;
    }

    @Override
    @Transactional
    public void reportComment(Long commentId, CommentReportRequest request, String guestKey)
    {
        requireComment(commentId);
        LoginUser loginUser = BlogCommentUtils.loginUserOrNull();
        BlogCommentReport report = new BlogCommentReport();
        report.setCommentId(commentId);
        report.setReporterUserId(loginUser == null ? null : loginUser.getUserId());
        report.setReporterGuestKey(loginUser == null ? guestKey : null);
        report.setReason(request.getReason());
        report.setStatus(BlogCommentConstants.REPORT_PENDING);
        reportMapper.insert(report);
    }

    @Override
    public Page<CommentVO> adminPage(CommentPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<>();
        if (query.getArticleId() != null)
        {
            wrapper.eq(BlogComment::getArticleId, query.getArticleId());
        }
        if (query.getStatus() != null)
        {
            wrapper.eq(BlogComment::getStatus, query.getStatus());
        }
        if (query.getAiStatus() != null)
        {
            wrapper.eq(BlogComment::getAiStatus, query.getAiStatus());
        }
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.like(BlogComment::getContent, query.getKeyword().trim());
        }
        wrapper.orderByDesc(BlogComment::getAiScore).orderByDesc(BlogComment::getCreateTime);
        Page<BlogComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> authorMap = loadAuthorNames(page.getRecords());
        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(c -> toVo(c, authorMap, null, null)).toList());
        return voPage;
    }

    @Override
    @Transactional
    public void audit(CommentAuditRequest request)
    {
        for (Long id : request.getIds())
        {
            BlogComment comment = requireComment(id);
            comment.setStatus(request.getStatus());
            comment.setRejectReason(request.getRejectReason());
            commentMapper.updateById(comment);
        }
    }

    @Override
    @Transactional
    public void deleteComment(Long id)
    {
        requireComment(id);
        commentMapper.deleteById(id);
    }

    private BlogComment buildComment(Long articleId, BlogComment parent, CommentCreateRequest request, LoginUser loginUser)
    {
        if (request.getContent().length() > commentConfigService.maxLength())
        {
            throw new ServiceException("评论内容过长");
        }
        FilterResult filterResult = sensitiveWordFilter.filter(request.getContent().trim());
        BlogComment comment = new BlogComment();
        comment.setArticleId(articleId);
        comment.setContent(filterResult.content());
        comment.setIp(IpUtils.getIpAddr());
        comment.setUserAgent(BlogCommentUtils.userAgent());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setAiStatus(BlogCommentConstants.AI_NOT_CHECKED);
        if (loginUser != null)
        {
            comment.setUserId(loginUser.getUserId());
            comment.setGuestName(loginUser.getUser().getNickName());
        }
        else
        {
            if (!commentConfigService.anonymousEnabled())
            {
                throw new ServiceException("请先登录后再评论");
            }
            if (!StringUtils.hasText(request.getGuestName()))
            {
                throw new ServiceException("请填写昵称");
            }
            comment.setGuestName(request.getGuestName().trim());
            comment.setGuestEmail(StringUtils.hasText(request.getGuestEmail()) ? request.getGuestEmail().trim() : null);
        }
        if (parent == null)
        {
            comment.setParentId(null);
            comment.setRootId(null);
        }
        else
        {
            comment.setParentId(parent.getId());
            comment.setRootId(parent.getRootId() == null ? parent.getId() : parent.getRootId());
        }
        int status = BlogCommentConstants.STATUS_PENDING;
        if (!commentConfigService.requireAudit())
        {
            status = BlogCommentConstants.STATUS_APPROVED;
        }
        if (filterResult.forceReview())
        {
            status = BlogCommentConstants.STATUS_PENDING;
        }
        comment.setStatus(status);
        comment.setSortScore(BlogCommentUtils.calcSortScore(comment));
        return comment;
    }

    private void checkRateLimit(Long userId)
    {
        int limit = commentConfigService.rateLimitPerMinute();
        String key = userId != null ? "blog:comment:rate:u:" + userId : "blog:comment:rate:ip:" + IpUtils.getIpAddr();
        Integer count = redisCache.getCacheObject(key);
        if (count != null && count >= limit)
        {
            throw new ServiceException("评论过于频繁，请稍后再试");
        }
        redisCache.setCacheObject(key, count == null ? 1 : count + 1, 1, TimeUnit.MINUTES);
    }

    private void requirePublishedArticle(Long articleId)
    {
        BlogArticle article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != STATUS_PUBLISHED)
        {
            throw new ServiceException("文章不存在或未发布", HttpStatus.NOT_FOUND);
        }
    }

    private BlogComment requireComment(Long id)
    {
        BlogComment comment = commentMapper.selectById(id);
        if (comment == null)
        {
            throw new ServiceException("评论不存在", HttpStatus.NOT_FOUND);
        }
        return comment;
    }

    private void incrementReplyCount(Long commentId)
    {
        BlogComment target = commentMapper.selectById(commentId);
        if (target != null)
        {
            target.setReplyCount((target.getReplyCount() == null ? 0 : target.getReplyCount()) + 1);
            commentMapper.updateById(target);
            refreshSortScore(commentId);
        }
    }

    private void refreshSortScore(Long commentId)
    {
        BlogComment comment = commentMapper.selectById(commentId);
        if (comment != null)
        {
            comment.setSortScore(BlogCommentUtils.calcSortScore(comment));
            commentMapper.updateById(comment);
        }
    }

    private void applySort(LambdaQueryWrapper<BlogComment> wrapper, String sort)
    {
        if (BlogCommentConstants.SORT_HOT.equalsIgnoreCase(sort))
        {
            wrapper.orderByDesc(BlogComment::getSortScore).orderByDesc(BlogComment::getCreateTime);
        }
        else
        {
            wrapper.orderByDesc(BlogComment::getCreateTime);
        }
    }

    private List<CommentVO> buildTree(List<BlogComment> roots, Long userId, String guestKey)
    {
        if (roots.isEmpty())
        {
            return List.of();
        }
        List<Long> rootIds = roots.stream().map(BlogComment::getId).toList();
        LambdaQueryWrapper<BlogComment> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.in(BlogComment::getRootId, rootIds)
                .isNotNull(BlogComment::getParentId)
                .eq(BlogComment::getStatus, BlogCommentConstants.STATUS_APPROVED)
                .orderByAsc(BlogComment::getCreateTime);
        List<BlogComment> replies = commentMapper.selectList(childWrapper);
        List<BlogComment> merged = new ArrayList<>(roots);
        merged.addAll(replies);
        Map<Long, String> authorMap = loadAuthorNames(merged);
        Set<Long> likedIds = loadLikedCommentIds(merged.stream().map(BlogComment::getId).toList(), userId, guestKey);
        Map<Long, CommentVO> voMap = new HashMap<>();
        for (BlogComment comment : merged)
        {
            CommentVO vo = toVo(comment, authorMap, userId, guestKey);
            vo.setLiked(likedIds.contains(comment.getId()));
            voMap.put(comment.getId(), vo);
        }
        List<CommentVO> result = new ArrayList<>();
        for (BlogComment root : roots)
        {
            CommentVO rootVo = voMap.get(root.getId());
            if (rootVo == null)
            {
                continue;
            }
            for (CommentVO vo : voMap.values())
            {
                if (root.getId().equals(vo.getRootId()) && vo.getParentId() != null)
                {
                    rootVo.getChildren().add(vo);
                }
            }
            result.add(rootVo);
        }
        return result;
    }

    private Map<Long, String> loadAuthorNames(List<BlogComment> comments)
    {
        Map<Long, String> map = new HashMap<>();
        for (BlogComment comment : comments)
        {
            if (comment.getUserId() != null)
            {
                SysUser user = sysUserService.selectUserById(comment.getUserId());
                if (user != null)
                {
                    map.put(comment.getUserId(), user.getNickName());
                }
            }
        }
        return map;
    }

    private Set<Long> loadLikedCommentIds(List<Long> commentIds, Long userId, String guestKey)
    {
        if (commentIds.isEmpty())
        {
            return Set.of();
        }
        Set<Long> liked = new HashSet<>();
        for (Long commentId : commentIds)
        {
            Long likeId = likeMapper.findLikeId(commentId, userId, guestKey);
            if (likeId != null)
            {
                liked.add(commentId);
            }
        }
        return liked;
    }

    private CommentVO toVo(BlogComment comment, Map<Long, String> authorMap, Long userId, String guestKey)
    {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setArticleId(comment.getArticleId());
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId());
        vo.setUserId(comment.getUserId());
        if (comment.getUserId() != null)
        {
            vo.setAuthorName(authorMap.getOrDefault(comment.getUserId(), comment.getGuestName()));
        }
        else
        {
            vo.setAuthorName(comment.getGuestName());
        }
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus());
        vo.setLikeCount(comment.getLikeCount());
        vo.setReplyCount(comment.getReplyCount());
        vo.setSortScore(comment.getSortScore());
        vo.setAiStatus(comment.getAiStatus());
        vo.setAiScore(comment.getAiScore());
        vo.setAiLabel(comment.getAiLabel());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
