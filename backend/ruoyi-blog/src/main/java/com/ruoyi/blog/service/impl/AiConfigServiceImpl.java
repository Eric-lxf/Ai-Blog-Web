package com.ruoyi.blog.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.domain.AiModuleConfig;
import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.dto.AiModuleConfigUpdateRequest;
import com.ruoyi.blog.dto.AiModuleOverrideSaveRequest;
import com.ruoyi.blog.mapper.AiModuleConfigMapper;
import com.ruoyi.blog.mapper.AiProviderMapper;
import com.ruoyi.blog.service.AiConfigService;
import com.ruoyi.blog.vo.AiFeatureModuleConfigItemVO;
import com.ruoyi.blog.vo.AiFeatureModuleConfigsVO;
import com.ruoyi.blog.vo.AiModuleConfigVO;
import com.ruoyi.blog.vo.AiProviderOptionVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService
{
    public static final String KEY_DEFAULT_PROVIDER = "ai.defaultProviderId";

    private final ISysConfigService sysConfigService;
    private final AiModuleConfigMapper aiModuleConfigMapper;
    private final AiProviderMapper aiProviderMapper;

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
    public AiFeatureModuleConfigsVO listFeatureModuleConfigs()
    {
        List<AiModuleConfig> overrides = aiModuleConfigMapper.selectList(new LambdaQueryWrapper<AiModuleConfig>()
                .in(AiModuleConfig::getModuleCode, AiModuleCode.all()));
        Map<String, AiModuleConfig> overrideMap = overrides.stream()
                .collect(Collectors.toMap(AiModuleConfig::getModuleCode, Function.identity(), (left, right) -> left));
        AiFeatureModuleConfigsVO vo = new AiFeatureModuleConfigsVO();
        vo.setModules(AiModuleCode.all().stream().map(moduleCode -> toModuleItem(moduleCode, overrideMap.get(moduleCode))).toList());
        vo.setProviderOptions(listProviderOptions());
        return vo;
    }

    @Override
    @Transactional
    public void saveFeatureModuleOverride(String moduleCode, AiModuleOverrideSaveRequest request)
    {
        validateModuleCode(moduleCode);
        AiProvider provider = aiProviderMapper.selectById(request.getProviderId());
        if (!isProviderUsable(provider))
        {
            throw new ServiceException("Provider 不可用，请选择已启用且已配置 API Key 的 Provider", HttpStatus.BAD_REQUEST);
        }
        AiModuleConfig existing = aiModuleConfigMapper.selectOne(new LambdaQueryWrapper<AiModuleConfig>()
                .eq(AiModuleConfig::getModuleCode, moduleCode)
                .last("LIMIT 1"));
        if (existing == null)
        {
            existing = new AiModuleConfig();
            existing.setModuleCode(moduleCode);
        }
        existing.setProviderId(request.getProviderId());
        existing.setTextModel(request.getTextModel());
        existing.setVisionModel(request.getVisionModel());
        existing.setTemperature(request.getTemperature());
        existing.setRemark(request.getRemark());
        if (existing.getId() == null)
        {
            aiModuleConfigMapper.insert(existing);
            return;
        }
        aiModuleConfigMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteFeatureModuleOverride(String moduleCode)
    {
        validateModuleCode(moduleCode);
        aiModuleConfigMapper.delete(new LambdaQueryWrapper<AiModuleConfig>().eq(AiModuleConfig::getModuleCode, moduleCode));
    }

    @Override
    public Long getDefaultProviderId()
    {
        String value = sysConfigService.selectConfigByKey(KEY_DEFAULT_PROVIDER);
        if (!StringUtils.hasText(value))
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

    private static AiFeatureModuleConfigItemVO toModuleItem(String moduleCode, AiModuleConfig override)
    {
        AiFeatureModuleConfigItemVO item = new AiFeatureModuleConfigItemVO();
        item.setModuleCode(moduleCode);
        item.setInherited(override == null);
        if (override != null)
        {
            item.setProviderId(override.getProviderId());
            item.setTextModel(override.getTextModel());
            item.setVisionModel(override.getVisionModel());
            item.setTemperature(override.getTemperature());
            item.setRemark(override.getRemark());
        }
        return item;
    }

    private static boolean isProviderUsable(AiProvider provider)
    {
        return provider != null && provider.getEnabled() != null && provider.getEnabled() == 1
                && StringUtils.hasText(provider.getApiKey());
    }

    private List<AiProviderOptionVO> listProviderOptions()
    {
        return aiProviderMapper.selectList(new LambdaQueryWrapper<AiProvider>().orderByDesc(AiProvider::getUpdateTime))
                .stream()
                .map(provider -> {
                    AiProviderOptionVO option = new AiProviderOptionVO();
                    option.setId(provider.getId());
                    option.setName(provider.getName());
                    option.setProviderType(provider.getProviderType());
                    option.setDefaultModel(provider.getDefaultModel());
                    option.setEnabled(provider.getEnabled());
                    return option;
                })
                .toList();
    }

    private static void validateModuleCode(String moduleCode)
    {
        if (!AiModuleCode.isSupported(moduleCode))
        {
            throw new ServiceException("非法模块编码: " + moduleCode, HttpStatus.BAD_REQUEST);
        }
    }
}
