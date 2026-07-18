package com.ruoyi.mall.product.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MallSpuSaveRequest
{
    private Long id;

    @NotNull(message = "商品类目不能为空")
    private Long categoryId;

    private Long brandId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称长度不能超过128")
    private String name;

    private String subtitle;
    private String mainImage;
    private String detailHtml;
    private String status;
    private Integer sort;
    private String remark;

    @Valid
    private List<MallSkuSaveRequest> skus;

    @Valid
    private List<MallSpuImageSaveRequest> images;
}
