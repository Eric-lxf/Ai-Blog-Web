package com.ruoyi.wechat.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatTagMarkRequest;
import com.ruoyi.wechat.dto.WechatTagSaveRequest;
import com.ruoyi.wechat.vo.WechatTagSyncResultVO;
import com.ruoyi.wechat.vo.WechatTagVO;

public interface WechatTagService
{
    Page<WechatTagVO> page(WechatPageQuery query);

    List<WechatTagVO> listByAccount(Long accountId);

    Long save(WechatTagSaveRequest request);

    void delete(Long id);

    WechatTagSyncResultVO syncFromWechat(Long accountId);

    void batchMark(WechatTagMarkRequest request);

    void applyFanTagsFromUserInfo(Long accountId, Long fansId, Object tagIdListObj);
}
