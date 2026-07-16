package com.ruoyi.blog.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class BillVO
{

    private Long id;
    private String tradeNo;
    private LocalDate billDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
