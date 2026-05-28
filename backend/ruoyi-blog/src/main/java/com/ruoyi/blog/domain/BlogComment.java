package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_comment")
public class BlogComment
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long rootId;
    private Long userId;
    private String guestName;
    private String guestEmail;
    private String content;
    private Integer status;
    private Integer likeCount;
    private Integer replyCount;
    private Double sortScore;
    private String ip;
    private String userAgent;
    private String rejectReason;
    private Integer aiStatus;
    private Integer aiScore;
    private String aiLabel;
    private LocalDateTime aiCheckedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
