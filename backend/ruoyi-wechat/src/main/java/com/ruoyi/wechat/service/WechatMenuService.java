package com.ruoyi.wechat.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatMenuSaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatMenuVO;

public interface WechatMenuService
{
    Page<WechatMenuVO> page(WechatPageQuery query);

    Long save(WechatMenuSaveRequest request);

    void publish(Long menuId);

    Map<String, Object> getFromWechat(Long accountId);

    void deleteFromWechat(Long accountId);

    Long syncFromWechat(Long accountId);

    void delete(Long menuId);
}
