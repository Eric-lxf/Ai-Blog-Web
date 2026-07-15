package com.ruoyi.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.domain.AiModuleConfig;
import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.mapper.AiModuleConfigMapper;
import com.ruoyi.blog.mapper.AiProviderMapper;
import com.ruoyi.blog.service.AiResolvedModelConfig.ConfigSource;
import com.ruoyi.blog.service.impl.AiProviderServiceImpl;
import com.ruoyi.blog.service.llm.LlmClient;
import com.ruoyi.common.exception.ServiceException;

import okhttp3.OkHttpClient;

@ExtendWith(MockitoExtension.class)
class AiProviderResolverTest
{
    @Mock
    private AiProviderMapper providerMapper;

    @Mock
    private AiConfigService aiConfigService;

    @Mock
    private AiModuleConfigMapper moduleConfigMapper;

    @Mock
    private LlmClient llmClient;

    @Mock
    private OkHttpClient deepSeekOkHttpClient;

    @InjectMocks
    private AiProviderServiceImpl service;

    @Test
    void moduleOverrideWinsAndUsesItsModelOverrides()
    {
        AiProvider provider = enabledProvider(9L, "module-default", "module-vision");
        AiModuleConfig override = new AiModuleConfig();
        override.setModuleCode(AiModuleCode.WRITE);
        override.setProviderId(9L);
        override.setTextModel("write-model");
        override.setTemperature(new BigDecimal("0.60"));

        when(moduleConfigMapper.selectOne(any())).thenReturn(override);
        when(providerMapper.selectById(9L)).thenReturn(provider);

        AiResolvedModelConfig resolved = service.resolveForModule(AiModuleCode.WRITE);

        assertEquals("write-model", resolved.getTextModel());
        assertEquals("module-vision", resolved.getVisionModel());
        assertEquals(new BigDecimal("0.60"), resolved.getTemperatureOverride());
        assertEquals(ConfigSource.MODULE_OVERRIDE, resolved.getSource());
    }

    @Test
    void invalidModuleProviderFallsBackToGlobalDefault()
    {
        AiModuleConfig override = new AiModuleConfig();
        override.setModuleCode(AiModuleCode.WRITE);
        override.setProviderId(9L);
        override.setTextModel("write-model");
        override.setVisionModel("write-vision");
        override.setTemperature(new BigDecimal("0.40"));

        AiProvider globalDefault = enabledProvider(11L, "global-default-model", "global-vision-model");

        when(moduleConfigMapper.selectOne(any())).thenReturn(override);
        when(providerMapper.selectById(9L)).thenReturn(null);
        when(aiConfigService.getDefaultProviderId()).thenReturn(11L);
        when(providerMapper.selectOne(any())).thenReturn(globalDefault);

        AiResolvedModelConfig resolved = service.resolveForModule(AiModuleCode.WRITE);

        assertEquals(globalDefault, resolved.getProvider());
        assertEquals("global-default-model", resolved.getTextModel());
        assertEquals("global-vision-model", resolved.getVisionModel());
        assertNull(resolved.getTemperatureOverride());
        assertEquals(ConfigSource.GLOBAL_DEFAULT, resolved.getSource());
    }

    @Test
    void selectsFirstEnabledProviderWhenGlobalDefaultMissing()
    {
        AiProvider firstEnabled = enabledProvider(7L, "first-default-model", "");

        when(moduleConfigMapper.selectOne(any())).thenReturn(null);
        when(aiConfigService.getDefaultProviderId()).thenReturn(null);
        when(providerMapper.selectOne(any())).thenReturn(firstEnabled);

        AiResolvedModelConfig resolved = service.resolveForModule(AiModuleCode.OPTIMIZE);

        assertEquals(firstEnabled, resolved.getProvider());
        assertEquals("first-default-model", resolved.getTextModel());
        assertEquals("first-default-model", resolved.getVisionModel());
        assertNull(resolved.getTemperatureOverride());
        assertEquals(ConfigSource.FIRST_ENABLED, resolved.getSource());
    }

    @Test
    void noDatabaseProviderFailsInsteadOfUsingYamlFallback()
    {
        when(moduleConfigMapper.selectOne(any())).thenReturn(null);
        when(aiConfigService.getDefaultProviderId()).thenReturn(null);
        when(providerMapper.selectOne(any())).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class, () -> service.resolveForModule(AiModuleCode.EDITOR));

        assertEquals("未配置可用 AI Provider", error.getMessage());
    }

    @Test
    void rejectsUnsupportedModuleCodeBeforeAnyDatabaseLookup()
    {
        ServiceException ex = assertThrows(ServiceException.class, () -> service.resolveForModule("chat"));

        assertEquals("非法模块编码: chat", ex.getMessage());
        verifyNoInteractions(moduleConfigMapper, providerMapper, aiConfigService);
    }

    private static AiProvider enabledProvider(Long id, String defaultModel, String visionModel)
    {
        AiProvider provider = new AiProvider();
        provider.setId(id);
        provider.setEnabled(1);
        provider.setApiKey("sk-test-key");
        provider.setDefaultModel(defaultModel);
        provider.setVisionModel(visionModel);
        return provider;
    }
}
