package com.ruoyi.blog.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BillVO
{

    private Long id;
    private String tradeNo;
    private LocalDate billDate;
    private LocalDateTime tradeTime;
    private String tradeType;
    private String direction;
    private String merchant;
    private String category;
    private BigDecimal amount;
    private String paymentMethod;
    private String merchantOrderNo;
    private String note;
    private String imageUrl;
    private Integer aiConfidence;
    /** 0-手动录入 1-AI识别 */
    private Integer source;
    private String sourceName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
