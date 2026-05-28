package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_sensitive_word")
public class BlogSensitiveWord
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private String word;
    private String matchMode;
    private String action;
    private String replaceText;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
