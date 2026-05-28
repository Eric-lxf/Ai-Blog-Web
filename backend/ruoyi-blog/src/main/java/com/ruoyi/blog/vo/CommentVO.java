package com.ruoyi.blog.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CommentVO
{
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long rootId;
    private Long userId;
    private String authorName;
    private String content;
    private Integer status;
    private Integer likeCount;
    private Integer replyCount;
    private Double sortScore;
    private Integer aiStatus;
    private Integer aiScore;
    private String aiLabel;
    private Boolean liked;
    private LocalDateTime createTime;
    private List<CommentVO> children = new ArrayList<>();
}
