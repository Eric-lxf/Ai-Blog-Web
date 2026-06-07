package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.domain.WechatMaterial;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatMaterialVO;

public interface WechatMaterialService
{
    Page<WechatMaterialVO> page(WechatPageQuery query);

    Long save(WechatMaterial material);

    void delete(Long id);
}
