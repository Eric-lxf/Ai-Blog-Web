package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_file")
public class BlogFile
{

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** OSS 对象 Key 或本地相对路径 */
    private String fileKey;

    /** 可访问的完整 URL */
    private String fileUrl;

    /** MIME 类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 所属用户 */
    private Long userId;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDeleted;
}