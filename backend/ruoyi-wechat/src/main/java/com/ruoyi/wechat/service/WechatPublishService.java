package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.domain.WechatPublishRecord;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.vo.WechatPublishRecordVO;

public interface WechatPublishService
{
    Page<WechatPublishRecordVO> page(WechatPageQuery query);

    Long save(WechatPublishRecord record);

    void markSuccess(Long id, String msgId, String responseBody);

    void markFailed(Long id, String errorMessage, String responseBody);
}
