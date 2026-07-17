package com.ruoyi.mall.product.vo;

import java.util.List;

import com.ruoyi.mall.product.domain.MallSku;
import com.ruoyi.mall.product.domain.MallSpuImage;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MallSpuDetailVO extends MallSpuVO
{
    private List<MallSku> skus;
    private List<MallSpuImage> images;
}
