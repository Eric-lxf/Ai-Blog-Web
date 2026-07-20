package com.ruoyi.mall.product.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class MallAttrOptionsReplaceRequest
{
    @Valid
    private List<MallAttrOptionSaveRequest> options = new ArrayList<>();
}
