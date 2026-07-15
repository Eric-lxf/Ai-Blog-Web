package com.ruoyi.blog.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.dto.AiProviderPageQuery;
import com.ruoyi.blog.dto.AiProviderSaveRequest;
import com.ruoyi.blog.vo.AiProviderOptionVO;
import com.ruoyi.blog.vo.AiProviderVO;

import okhttp3.OkHttpClient;

public interface AiProviderService
{
    Page<AiProviderVO> page(AiProviderPageQuery query);

    List<AiProviderOptionVO> listOptions();

    AiProviderVO getById(Long id);

    Long save(AiProviderSaveRequest request);

    void delete(Long id);

    void testConnection(Long id);

    /**
     * 解析当前生效的 Provider：优先默认 ID，其次首个启用行（仅数据库）。
     */
    AiProvider resolveActiveProvider();

    AiResolvedModelConfig resolveForModule(String moduleCode);

    boolean isConfigured();

    OkHttpClient httpClient(AiProvider provider);
}
