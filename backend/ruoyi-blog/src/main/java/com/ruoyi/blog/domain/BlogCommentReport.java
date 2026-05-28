package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_comment_report")
public class BlogCommentReport
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long commentId;
    private Long reporterUserId;
    private String reporterGuestKey;
    private String reason;
    private Integer status;
    private String handleRemark;
    private String handleBy;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
