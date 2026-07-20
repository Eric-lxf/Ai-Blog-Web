package com.ruoyi.mall.product.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MallCategoryAttrBindRequest
{
    @Valid
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item
    {
        @NotNull(message = "属性ID不能为空")
        private Long attrId;

        @NotBlank(message = "属性类型不能为空")
        private String attrType;

        private String required;
        private Integer sort;
    }
}
