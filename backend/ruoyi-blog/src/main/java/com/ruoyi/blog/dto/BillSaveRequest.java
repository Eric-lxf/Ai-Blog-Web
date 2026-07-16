package com.ruoyi.blog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class BillSaveRequest
{

    /** 非 null 时为更新 */
    private Long id;

    private String tradeNo;

    @NotNull(message = "消费日期不能为空")
    private LocalDate billDate;

    private LocalDateTime tradeTime;
    private String tradeType;
    /** 收/支/其他，默认支出 */
    private String direction;

    private String merchant;

    @NotBlank(message = "消费类目不能为空")
    private String category;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    private String paymentMethod;
    private String merchantOrderNo;
    private String note;
    private String imageUrl;
    private Integer aiConfidence;
    /** 0-手动录入 1-AI识别；默认 0 */
    private Integer source;
}
