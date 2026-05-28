package com.ruoyi.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateRequest
{
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容过长")
    private String content;

    @Size(max = 64, message = "昵称过长")
    private String guestName;

    @Size(max = 128, message = "邮箱过长")
    private String guestEmail;

    /** 回复时传父评论 ID */
    private Long parentId;
}
