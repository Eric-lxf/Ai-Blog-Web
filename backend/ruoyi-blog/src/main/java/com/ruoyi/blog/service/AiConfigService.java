package com.ruoyi.blog.service;

import com.ruoyi.blog.dto.AiModuleConfigUpdateRequest;
import com.ruoyi.blog.vo.AiModuleConfigVO;

public interface AiConfigService
{
    AiModuleConfigVO getModuleConfig();

    void updateModuleConfig(AiModuleConfigUpdateRequest request);

    Long getDefaultProviderId();
}
