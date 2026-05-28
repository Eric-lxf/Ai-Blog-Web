package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_comment_like")
public class BlogCommentLike
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long userId;
    private String guestKey;
    private LocalDateTime createTime;
}
