package com.ruoyi.mall.product.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_spu_image")
public class MallSpuImage
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long spuId;
    private String url;
    private Integer sort;
    private LocalDateTime createTime;
}
