package com.ruoyi.blog.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ruoyi.blog.event.CommentApprovedEvent;
import com.ruoyi.blog.service.BlogNotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommentApprovedNotificationListener
{
    private final BlogNotificationService blogNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentApproved(CommentApprovedEvent event)
    {
        blogNotificationService.onCommentApproved(event.getCommentId());
    }
}
