package com.ruoyi.wechat.service;

import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatMassPreviewRequest;
import com.ruoyi.wechat.dto.WechatMassSendRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatMassRecordVO;

public interface WechatMassMessageService
{
    Page<WechatMassRecordVO> page(WechatPageQuery query);

    Map<String, Object> preview(WechatMassPreviewRequest request);

    Long send(WechatMassSendRequest request);

    Map<String, Object> syncStatus(Long recordId);
}
