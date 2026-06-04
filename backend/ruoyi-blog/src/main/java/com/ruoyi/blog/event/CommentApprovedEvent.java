package com.ruoyi.blog.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentApprovedEvent
{
    private final Long commentId;
}
