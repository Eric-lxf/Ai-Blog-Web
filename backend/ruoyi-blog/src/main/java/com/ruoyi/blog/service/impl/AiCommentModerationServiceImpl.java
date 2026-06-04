package com.ruoyi.blog.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.blog.constant.BlogCommentConstants;
import com.ruoyi.blog.domain.BlogComment;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.mapper.BlogCommentMapper;
import com.ruoyi.blog.service.AiCommentModerationService;
import com.ruoyi.blog.service.CommentApprovedEventPublisher;
import com.ruoyi.blog.service.CommentConfigService;
import com.ruoyi.blog.service.DeepSeekService;
import com.ruoyi.common.utils.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommentModerationServiceImpl implements AiCommentModerationService
{
    private static final Pattern LINK_PATTERN = Pattern.compile("https?://|www\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEAT_PATTERN = Pattern.compile("(.)\\1{6,}");

    private final BlogCommentMapper commentMapper;
    private final DeepSeekService deepSeekService;
    private final CommentConfigService commentConfigService;
    private final CommentApprovedEventPublisher commentApprovedEventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Async("aiTaskExecutor")
    public void reviewAsync(Long commentId)
    {
        BlogComment comment = commentMapper.selectById(commentId);
        if (comment == null)
        {
            return;
        }
        comment.setAiStatus(BlogCommentConstants.AI_CHECKING);
        commentMapper.updateById(comment);
        int ruleScore = evaluateRuleScore(comment.getContent());
        try
        {
            if (!commentConfigService.aiEnabled())
            {
                applyResult(comment, ruleScore, "[]", "review", "规则检测");
                return;
            }
            AiCompletionRequest request = new AiCompletionRequest();
            request.setScene("COMMENT_MODERATE");
            request.setPrompt("评论内容：" + comment.getContent());
            String raw = deepSeekService.chatCompletion(request);
            JsonNode node = parseJson(raw);
            int aiScore = node.path("riskScore").asInt(ruleScore);
            int finalScore = Math.max(ruleScore, aiScore);
            String labels = node.path("labels").toString();
            String suggestion = node.path("suggestion").asText("review");
            String reason = node.path("reason").asText("");
            applyResult(comment, finalScore, labels, suggestion, reason);
        }
        catch (Exception ex)
        {
            log.warn("AI comment moderation failed, commentId={}", commentId, ex);
            applyResult(comment, ruleScore, "[]", "review", "AI检测失败");
        }
    }

    private int evaluateRuleScore(String content)
    {
        if (!StringUtils.hasText(content))
        {
            return 0;
        }
        int score = 0;
        if (LINK_PATTERN.matcher(content).find())
        {
            score += 35;
        }
        if (REPEAT_PATTERN.matcher(content).find())
        {
            score += 25;
        }
        if (content.length() < 4)
        {
            score += 20;
        }
        long unique = content.chars().distinct().count();
        if (content.length() > 20 && unique < 5)
        {
            score += 30;
        }
        return Math.min(score, 100);
    }

    private void applyResult(BlogComment comment, int score, String labels, String suggestion, String reason)
    {
        Integer previousStatus = comment.getStatus();
        comment.setAiScore(score);
        comment.setAiLabel(labels);
        comment.setAiCheckedTime(LocalDateTime.now());
        int rejectScore = commentConfigService.aiAutoRejectScore();
        if (score >= rejectScore || "reject".equalsIgnoreCase(suggestion))
        {
            comment.setAiStatus(BlogCommentConstants.AI_HIGH_RISK);
            comment.setStatus(BlogCommentConstants.STATUS_SPAM);
            comment.setRejectReason(StringUtils.isEmpty(reason) ? "AI/规则判定为高风险" : reason);
        }
        else if (score >= 50 || "review".equalsIgnoreCase(suggestion))
        {
            comment.setAiStatus(BlogCommentConstants.AI_SUSPICIOUS);
            comment.setStatus(BlogCommentConstants.STATUS_PENDING);
        }
        else
        {
            comment.setAiStatus(BlogCommentConstants.AI_PASS);
            if (!commentConfigService.requireAudit())
            {
                comment.setStatus(BlogCommentConstants.STATUS_APPROVED);
            }
        }
        commentMapper.updateById(comment);
        if (Objects.equals(comment.getStatus(), BlogCommentConstants.STATUS_APPROVED)
                && !Objects.equals(previousStatus, BlogCommentConstants.STATUS_APPROVED))
        {
            commentApprovedEventPublisher.publish(comment.getId());
        }
    }

    private JsonNode parseJson(String raw)
    {
        try
        {
            String trimmed = raw == null ? "{}" : raw.trim();
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start)
            {
                trimmed = trimmed.substring(start, end + 1);
            }
            return objectMapper.readTree(trimmed);
        }
        catch (Exception ex)
        {
            return objectMapper.createObjectNode();
        }
    }
}
