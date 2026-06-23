package com.ruoyi.blog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("blog_bill")
public class BlogBill
{

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate billDate;
    private String merchant;
    private String category;
    private BigDecimal amount;
    private String paymentMethod;
    private String note;
    private String imageUrl;
    /** 0-100 */
    private Integer aiConfidence;
    /** 0-手动录入 1-AI识别 */
    private Integer source;
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}