package com.ruoyi.mall.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressSaveRequest
{
    private Long id;

    @NotBlank(message = "收货人不能为空")
    @Size(max = 64, message = "收货人不能超过64个字符")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String mobile;

    @NotBlank(message = "省不能为空")
    @Size(max = 64, message = "省不能超过64个字符")
    private String province;

    @NotBlank(message = "市不能为空")
    @Size(max = 64, message = "市不能超过64个字符")
    private String city;

    @NotBlank(message = "区县不能为空")
    @Size(max = 64, message = "区县不能超过64个字符")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过255个字符")
    private String detail;

    private String isDefault;
}
