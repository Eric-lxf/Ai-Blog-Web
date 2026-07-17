package com.ruoyi.mall.product.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("mall_spu")
public class MallSpu
{
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long brandId;
    private String name;
    private String subtitle;
    private String mainImage;
    private String detailHtml;
    private String status;
    private Integer sort;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;
    private String delFlag;
}
