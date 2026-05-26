package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_tag")
public class BlogTag
{

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDateTime createTime;
}
