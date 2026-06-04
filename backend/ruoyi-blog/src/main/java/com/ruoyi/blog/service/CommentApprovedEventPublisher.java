package com.ruoyi.blog.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.blog.event.CommentApprovedEvent;

import lombok.RequiredArgsConstructor;

/**
 * 在事务提交后发布评论审核通过事件，供通知监听器消费。
 */
@Service
@RequiredArgsConstructor
public class CommentApprovedEventPublisher
{
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void publish(Long commentId)
    {
        if (commentId == null)
        {
            return;
        }
        eventPublisher.publishEvent(new CommentApprovedEvent(commentId));
    }
}
