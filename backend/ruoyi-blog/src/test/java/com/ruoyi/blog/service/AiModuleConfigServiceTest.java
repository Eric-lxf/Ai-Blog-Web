package com.ruoyi.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.domain.AiModuleConfig;
import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.dto.AiModuleOverrideSaveRequest;
import com.ruoyi.blog.mapper.AiModuleConfigMapper;
import com.ruoyi.blog.mapper.AiProviderMapper;
import com.ruoyi.blog.service.impl.AiConfigServiceImpl;
import com.ruoyi.blog.service.impl.AiProviderServiceImpl;
import com.ruoyi.blog.service.llm.LlmClient;
import com.ruoyi.blog.vo.AiFeatureModuleConfigsVO;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.ISysConfigService;

import okhttp3.OkHttpClient;

@ExtendWith(MockitoExtension.class)
class AiModuleConfigServiceTest
{
    @Mock
    private ISysConfigService sysConfigService;

    @Mock
    private AiModuleConfigMapper moduleConfigMapper;

    @Mock
    private AiProviderMapper providerMapper;

    @Mock
    private AiConfigService aiConfigService;

    @Mock
    private LlmClient llmClient;

    @Mock
    private OkHttpClient deepSeekOkHttpClient;

    private AiConfigServiceImpl configService;
    private AiProviderServiceImpl providerService;

    @BeforeEach
    void setUp()
    {
        configService = new AiConfigServiceImpl(sysConfigService, moduleConfigMapper, providerMapper);
        providerService = new AiProviderServiceImpl(moduleConfigMapper, providerMapper, aiConfigService, llmClient,
                deepSeekOkHttpClient);
    }

    @Test
    void saveFeatureModuleOverrideRejectsMissingProvider()
    {
        when(providerMapper.selectById(99L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> configService.saveFeatureModuleOverride(AiModuleCode.WRITE, requestWithProviderId(99L)));

        assertTrue(ex.getMessage().contains("Provider"));
    }

    @Test
    void saveFeatureModuleOverrideRejectsDisabledProvider()
    {
        when(providerMapper.selectById(99L)).thenReturn(provider(99L, 0, "sk-disabled"));

        assertThrows(ServiceException.class,
                () -> configService.saveFeatureModuleOverride(AiModuleCode.WRITE, requestWithProviderId(99L)));
    }

    @Test
    void saveFeatureModuleOverrideRejectsProviderWithoutApiKey()
    {
        when(providerMapper.selectById(99L)).thenReturn(provider(99L, 1, " "));

        assertThrows(ServiceException.class,
                () -> configService.saveFeatureModuleOverride(AiModuleCode.WRITE, requestWithProviderId(99L)));
    }

    @Test
    void deleteFeatureModuleOverrideRestoresInheritance()
    {
        configService.deleteFeatureModuleOverride(AiModuleCode.WRITE);

        verify(moduleConfigMapper).delete(any());
    }

    @Test
    void listFeatureModuleConfigsAlwaysReturnsSixModules()
    {
        AiModuleConfig writeOverride = new AiModuleConfig();
        writeOverride.setModuleCode(AiModuleCode.WRITE);
        writeOverride.setProviderId(9L);
        writeOverride.setTextModel("write-model");
        writeOverride.setVisionModel("write-vision");
        writeOverride.setTemperature(new BigDecimal("0.70"));
        writeOverride.setRemark("write-remark");
        AiProvider provider = provider(9L, 1, "sk-provider");
        provider.setName("Provider-9");
        provider.setProviderType("openai_compatible");
        when(moduleConfigMapper.selectList(any())).thenReturn(List.of(writeOverride));
        when(providerMapper.selectList(any())).thenReturn(List.of(provider));

        AiFeatureModuleConfigsVO result = configService.listFeatureModuleConfigs();

        assertEquals(AiModuleCode.all(),
                result.getModules().stream().map(item -> item.getModuleCode()).toList());
        assertTrue(result.getModules().get(0).getInherited());
        assertFalse(result.getModules().get(1).getInherited());
        assertEquals(9L, result.getModules().get(1).getProviderId());
        assertEquals(1, result.getProviderOptions().size());
    }

    @Test
    void providerDeleteRejectsWhenReferencedByModuleConfig()
    {
        AiModuleConfig override = new AiModuleConfig();
        override.setModuleCode(AiModuleCode.WRITE);
        override.setProviderId(9L);
        when(moduleConfigMapper.selectList(any())).thenReturn(List.of(override));

        ServiceException ex = assertThrows(ServiceException.class, () -> providerService.delete(9L));

        assertTrue(ex.getMessage().contains(AiModuleCode.WRITE));
        verify(providerMapper, never()).deleteById(9L);
    }

    private static AiModuleOverrideSaveRequest requestWithProviderId(Long providerId)
    {
        AiModuleOverrideSaveRequest request = new AiModuleOverrideSaveRequest();
        request.setProviderId(providerId);
        request.setTextModel("gpt-test");
        request.setVisionModel("gpt-vision-test");
        request.setTemperature(new BigDecimal("0.60"));
        request.setRemark("remark");
        return request;
    }

    private static AiProvider provider(Long id, Integer enabled, String apiKey)
    {
        AiProvider provider = new AiProvider();
        provider.setId(id);
        provider.setEnabled(enabled);
        provider.setApiKey(apiKey);
        provider.setDefaultModel("model");
        return provider;
    }

}
