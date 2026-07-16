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

    /** 交易单号 */
    private String tradeNo;
    private LocalDate billDate;
    /** 交易时间 */
    private LocalDateTime tradeTime;
    /** 交易类型：商户消费/转账等 */
    private String tradeType;
    /** 收/支/其他 */
    private String direction;
    /** 交易对方/商户 */
    private String merchant;
    private String category;
    private BigDecimal amount;
    /** 交易方式 */
    private String paymentMethod;
    /** 商户单号 */
    private String merchantOrderNo;
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
