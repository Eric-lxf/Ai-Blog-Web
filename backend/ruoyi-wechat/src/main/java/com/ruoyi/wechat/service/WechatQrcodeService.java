package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.dto.WechatPageQuery;
import com.ruoyi.wechat.dto.WechatQrcodeCreateRequest;
import com.ruoyi.wechat.vo.WechatQrcodeVO;

public interface WechatQrcodeService
{
    Page<WechatQrcodeVO> page(WechatPageQuery query);

    Long create(WechatQrcodeCreateRequest request);

    void delete(Long id);

    void recordScan(Long accountId, String event, String eventKey);
}
