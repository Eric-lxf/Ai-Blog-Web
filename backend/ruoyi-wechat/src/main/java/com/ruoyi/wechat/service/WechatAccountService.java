package com.ruoyi.wechat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.wechat.domain.WechatAccount;
import com.ruoyi.wechat.dto.WechatAccountSaveRequest;
import com.ruoyi.wechat.dto.WechatPageQuery;
import java.util.List;

import com.ruoyi.wechat.vo.WechatAccountOptionVO;
import com.ruoyi.wechat.vo.WechatAccountVO;

public interface WechatAccountService
{
    Page<WechatAccountVO> page(WechatPageQuery query);

    List<WechatAccountOptionVO> listOptions();

    WechatAccountVO getById(Long id);

    Long save(WechatAccountSaveRequest request);

    void delete(Long id);

    WechatAccount getEnabledAccount(Long accountId);

    void testConnection(Long id);
}
