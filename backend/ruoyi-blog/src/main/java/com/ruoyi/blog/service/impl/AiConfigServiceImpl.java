package com.ruoyi.blog.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ruoyi.blog.dto.AiModuleConfigUpdateRequest;
import com.ruoyi.blog.service.AiConfigService;
import com.ruoyi.blog.vo.AiModuleConfigVO;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService
{
    public static final String KEY_DEFAULT_PROVIDER = "ai.defaultProviderId";

    private final ISysConfigService sysConfigService;

    @Override
    public AiModuleConfigVO getModuleConfig()
    {
        AiModuleConfigVO vo = new AiModuleConfigVO();
        vo.setDefaultProviderId(sysConfigService.selectConfigByKey(KEY_DEFAULT_PROVIDER));
        if (vo.getDefaultProviderId() == null)
        {
            vo.setDefaultProviderId("");
        }
        return vo;
    }

    @Override
    public void updateModuleConfig(AiModuleConfigUpdateRequest request)
    {
        String value = request.getDefaultProviderId() == null ? "" : request.getDefaultProviderId().trim();
        upsertConfig(KEY_DEFAULT_PROVIDER, value, "AI默认Provider", "为空时使用首个启用的 Provider");
    }

    @Override
    public Long getDefaultProviderId()
    {
        String value = sysConfigService.selectConfigByKey(KEY_DEFAULT_PROVIDER);
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        try
        {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private void upsertConfig(String key, String value, String name, String remark)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        List<SysConfig> configs = sysConfigService.selectConfigList(query);
        if (configs.isEmpty())
        {
            SysConfig config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("Y");
            config.setRemark(remark);
            sysConfigService.insertConfig(config);
            return;
        }
        SysConfig config = configs.get(0);
        config.setConfigValue(value);
        sysConfigService.updateConfig(config);
    }
}
