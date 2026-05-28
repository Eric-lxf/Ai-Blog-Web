package com.ruoyi.blog.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CommentReportVO
{
    private Long id;
    private Long commentId;
    private String commentContent;
    private String articleTitle;
    private String reason;
    private Integer status;
    private String handleRemark;
    private String handleBy;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
