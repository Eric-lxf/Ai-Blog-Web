package com.ruoyi.blog.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.config.DeepSeekProperties;
import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.constant.AiProviderType;
import com.ruoyi.blog.domain.AiModuleConfig;
import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.dto.AiProviderPageQuery;
import com.ruoyi.blog.dto.AiProviderSaveRequest;
import com.ruoyi.blog.mapper.AiModuleConfigMapper;
import com.ruoyi.blog.mapper.AiProviderMapper;
import com.ruoyi.blog.service.AiConfigService;
import com.ruoyi.blog.service.AiProviderService;
import com.ruoyi.blog.service.AiResolvedModelConfig;
import com.ruoyi.blog.service.AiResolvedModelConfig.ConfigSource;
import com.ruoyi.blog.service.llm.LlmClient;
import com.ruoyi.blog.vo.AiProviderOptionVO;
import com.ruoyi.blog.vo.AiProviderVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;

@Service
@RequiredArgsConstructor
public class AiProviderServiceImpl implements AiProviderService
{
    private static final String MASK_PLACEHOLDER = "********";

    private final AiModuleConfigMapper aiModuleConfigMapper;
    private final AiProviderMapper aiProviderMapper;
    private final AiConfigService aiConfigService;
    private final DeepSeekProperties deepSeekProperties;
    private final LlmClient llmClient;
    private final OkHttpClient deepSeekOkHttpClient;

    @Override
    public Page<AiProviderVO> page(AiProviderPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        Page<AiProvider> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiProvider> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword()))
        {
            wrapper.and(w -> w.like(AiProvider::getName, query.getKeyword())
                    .or().like(AiProvider::getDefaultModel, query.getKeyword()));
        }
        if (query.getStatus() != null)
        {
            wrapper.eq(AiProvider::getEnabled, query.getStatus());
        }
        if (StringUtils.hasText(query.getProviderType()))
        {
            wrapper.eq(AiProvider::getProviderType, query.getProviderType());
        }
        wrapper.orderByDesc(AiProvider::getUpdateTime);
        Page<AiProvider> result = aiProviderMapper.selectPage(page, wrapper);
        Page<AiProviderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public List<AiProviderOptionVO> listOptions()
    {
        return aiProviderMapper.selectList(new LambdaQueryWrapper<AiProvider>().orderByDesc(AiProvider::getUpdateTime))
                .stream()
                .map(p -> {
                    AiProviderOptionVO vo = new AiProviderOptionVO();
                    vo.setId(p.getId());
                    vo.setName(p.getName());
                    vo.setProviderType(p.getProviderType());
                    vo.setDefaultModel(p.getDefaultModel());
                    vo.setEnabled(p.getEnabled());
                    return vo;
                })
                .toList();
    }

    @Override
    public AiProviderVO getById(Long id)
    {
        return toVO(requireById(id));
    }

    @Override
    @Transactional
    public Long save(AiProviderSaveRequest request)
    {
        if (!AiProviderType.isSupported(request.getProviderType()))
        {
            throw new ServiceException("不支持的厂商类型，请使用 openai_compatible 或 anthropic", HttpStatus.BAD_REQUEST);
        }
        normalizeBaseUrl(request);
        normalizeAuthMode(request);

        if (request.getId() == null)
        {
            if (!StringUtils.hasText(request.getApiKey()))
            {
                throw new ServiceException("API Key 不能为空", HttpStatus.BAD_REQUEST);
            }
            AiProvider provider = new AiProvider();
            BeanUtils.copyProperties(request, provider);
            provider.setApiKey(request.getApiKey().trim());
            provider.setBaseUrl(trimSlash(request.getBaseUrl()));
            aiProviderMapper.insert(provider);
            return provider.getId();
        }

        AiProvider existing = requireById(request.getId());
        existing.setName(request.getName());
        existing.setProviderType(request.getProviderType());
        existing.setAuthMode(request.getAuthMode());
        existing.setBaseUrl(trimSlash(request.getBaseUrl()));
        existing.setDefaultModel(request.getDefaultModel());
        existing.setVisionModel(request.getVisionModel());
        existing.setTimeoutSeconds(request.getTimeoutSeconds());
        existing.setEnabled(request.getEnabled());
        existing.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getApiKey()) && !isMaskedKey(request.getApiKey()))
        {
            existing.setApiKey(request.getApiKey().trim());
        }
        aiProviderMapper.updateById(existing);
        return existing.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        if (aiProviderMapper.deleteById(id) == 0)
        {
            throw new ServiceException("AI Provider 不存在", HttpStatus.NOT_FOUND);
        }
        Long defaultId = aiConfigService.getDefaultProviderId();
        if (defaultId != null && defaultId.equals(id))
        {
            com.ruoyi.blog.dto.AiModuleConfigUpdateRequest clear = new com.ruoyi.blog.dto.AiModuleConfigUpdateRequest();
            clear.setDefaultProviderId("");
            aiConfigService.updateModuleConfig(clear);
        }
    }

    @Override
    public void testConnection(Long id)
    {
        AiProvider provider = requireById(id);
        if (provider.getEnabled() == null || provider.getEnabled() != 1)
        {
            throw new ServiceException("请先启用该 Provider 再测试", HttpStatus.BAD_REQUEST);
        }
        llmClient.testConnection(provider, httpClient(provider));
    }

    @Override
    public AiProvider resolveActiveProvider()
    {
        return resolveDatabaseProvider().provider();
    }

    @Override
    public AiResolvedModelConfig resolveForModule(String moduleCode)
    {
        if (!AiModuleCode.isSupported(moduleCode))
        {
            throw new ServiceException("非法模块编码: " + moduleCode, HttpStatus.BAD_REQUEST);
        }
        AiModuleConfig moduleConfig = aiModuleConfigMapper.selectOne(new LambdaQueryWrapper<AiModuleConfig>()
                .eq(AiModuleConfig::getModuleCode, moduleCode)
                .last("LIMIT 1"));
        if (moduleConfig != null && moduleConfig.getProviderId() != null)
        {
            AiProvider moduleProvider = aiProviderMapper.selectById(moduleConfig.getProviderId());
            if (isProviderUsable(moduleProvider))
            {
                return buildResolvedConfig(moduleConfig, moduleProvider, ConfigSource.MODULE_OVERRIDE);
            }
        }

        ProviderSelection selection = resolveDatabaseProvider();
        if (selection.provider() == null)
        {
            throw new ServiceException("未配置可用 AI Provider", HttpStatus.ERROR);
        }
        return buildResolvedConfig(null, selection.provider(), selection.source());
    }

    @Override
    public boolean isConfigured()
    {
        return resolveActiveProvider() != null;
    }

    private AiProvider requireById(Long id)
    {
        AiProvider provider = aiProviderMapper.selectById(id);
        if (provider == null)
        {
            throw new ServiceException("AI Provider 不存在", HttpStatus.NOT_FOUND);
        }
        return provider;
    }

    private AiProviderVO toVO(AiProvider provider)
    {
        AiProviderVO vo = new AiProviderVO();
        BeanUtils.copyProperties(provider, vo);
        vo.setApiKeyMasked(maskKey(provider.getApiKey()));
        return vo;
    }

    static String maskKey(String apiKey)
    {
        if (!StringUtils.hasText(apiKey))
        {
            return "";
        }
        String key = apiKey.trim();
        if (key.length() <= 8)
        {
            return MASK_PLACEHOLDER;
        }
        return key.substring(0, 3) + MASK_PLACEHOLDER + key.substring(key.length() - 4);
    }

    private static boolean isMaskedKey(String apiKey)
    {
        return apiKey != null && apiKey.contains(MASK_PLACEHOLDER);
    }

    private void normalizeBaseUrl(AiProviderSaveRequest request)
    {
        if (AiProviderType.isOpenAiCompatible(request.getProviderType()) && !StringUtils.hasText(request.getBaseUrl()))
        {
            request.setBaseUrl("https://api.openai.com");
        }
        if (AiProviderType.isAnthropic(request.getProviderType()) && !StringUtils.hasText(request.getBaseUrl()))
        {
            request.setBaseUrl("https://api.anthropic.com");
        }
    }

    private void normalizeAuthMode(AiProviderSaveRequest request)
    {
        if (!AiProviderType.isAnthropic(request.getProviderType()))
        {
            request.setAuthMode(AiProviderType.AUTH_MODE_API_KEY);
            return;
        }
        if (!StringUtils.hasText(request.getAuthMode()))
        {
            request.setAuthMode(AiProviderType.AUTH_MODE_API_KEY);
            return;
        }
        String authMode = request.getAuthMode().trim().toLowerCase();
        if (!AiProviderType.isSupportedAnthropicAuthMode(authMode))
        {
            throw new ServiceException("Claude 认证方式仅支持 api_key 或 auth_token", HttpStatus.BAD_REQUEST);
        }
        request.setAuthMode(authMode);
    }

    private static String trimSlash(String url)
    {
        if (url == null)
        {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    @Override
    public OkHttpClient httpClient(AiProvider provider)
    {
        int timeout = provider.getTimeoutSeconds() == null ? 300 : provider.getTimeoutSeconds();
        if (timeout == deepSeekProperties.getTimeoutSeconds())
        {
            return deepSeekOkHttpClient;
        }
        return deepSeekOkHttpClient.newBuilder().readTimeout(timeout, TimeUnit.SECONDS).build();
    }

    private ProviderSelection resolveDatabaseProvider()
    {
        Long defaultId = aiConfigService.getDefaultProviderId();
        if (defaultId != null)
        {
            AiProvider preferred = aiProviderMapper.selectOne(new LambdaQueryWrapper<AiProvider>()
                    .eq(AiProvider::getId, defaultId)
                    .eq(AiProvider::getEnabled, 1));
            if (isProviderUsable(preferred))
            {
                return new ProviderSelection(preferred, ConfigSource.GLOBAL_DEFAULT);
            }
        }
        AiProvider first = aiProviderMapper.selectOne(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getEnabled, 1)
                .orderByAsc(AiProvider::getId)
                .last("LIMIT 1"));
        if (isProviderUsable(first))
        {
            return new ProviderSelection(first, ConfigSource.FIRST_ENABLED);
        }
        return new ProviderSelection(null, null);
    }

    private AiResolvedModelConfig buildResolvedConfig(AiModuleConfig moduleConfig, AiProvider provider, ConfigSource source)
    {
        AiModuleConfig config = moduleConfig == null ? new AiModuleConfig() : moduleConfig;
        String textModel = StringUtils.hasText(config.getTextModel()) ? config.getTextModel() : provider.getDefaultModel();
        String visionModel = StringUtils.hasText(config.getVisionModel()) ? config.getVisionModel()
                : StringUtils.hasText(provider.getVisionModel()) ? provider.getVisionModel() : provider.getDefaultModel();
        return new AiResolvedModelConfig(provider, textModel, visionModel,
                moduleConfig == null ? null : moduleConfig.getTemperature(), source);
    }

    private boolean isProviderUsable(AiProvider provider)
    {
        return provider != null && provider.getEnabled() != null && provider.getEnabled() == 1
                && StringUtils.hasText(provider.getApiKey());
    }

    private record ProviderSelection(AiProvider provider, ConfigSource source)
    {
    }
}
