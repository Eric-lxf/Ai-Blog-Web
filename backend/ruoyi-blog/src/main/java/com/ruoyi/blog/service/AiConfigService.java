package com.ruoyi.blog.service;

import com.ruoyi.blog.dto.AiModuleConfigUpdateRequest;
import com.ruoyi.blog.dto.AiModuleOverrideSaveRequest;
import com.ruoyi.blog.vo.AiFeatureModuleConfigsVO;
import com.ruoyi.blog.vo.AiModuleConfigVO;

public interface AiConfigService
{
    AiModuleConfigVO getModuleConfig();

    void updateModuleConfig(AiModuleConfigUpdateRequest request);

    AiFeatureModuleConfigsVO listFeatureModuleConfigs();

    void saveFeatureModuleOverride(String moduleCode, AiModuleOverrideSaveRequest request);

    void deleteFeatureModuleOverride(String moduleCode);

    Long getDefaultProviderId();
}
