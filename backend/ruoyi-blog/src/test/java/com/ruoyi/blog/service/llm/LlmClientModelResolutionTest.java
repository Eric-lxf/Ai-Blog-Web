package com.ruoyi.blog.service.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.domain.AiProvider;

class LlmClientModelResolutionTest
{
    @Test
    void usesResolvedModelInsteadOfLegacyTemplateModel()
    {
        AiProvider provider = new AiProvider();
        provider.setDefaultModel("provider-default");
        AiPromptTemplate template = new AiPromptTemplate();
        template.setModelName("legacy-template-model");

        assertEquals("resolved-model", LlmClientImpl.resolveTextModel("resolved-model", provider, template));
    }

    @Test
    void fallsBackToProviderDefaultWhenResolvedTextModelMissing()
    {
        AiProvider provider = new AiProvider();
        provider.setDefaultModel("provider-default");
        AiPromptTemplate template = new AiPromptTemplate();
        template.setModelName("legacy-template-model");

        assertEquals("provider-default", LlmClientImpl.resolveTextModel(null, provider, template));
    }

    @Test
    void fallsBackToVisionThenDefaultWhenResolvedVisionModelMissing()
    {
        AiProvider provider = new AiProvider();
        provider.setDefaultModel("provider-default");
        provider.setVisionModel("provider-vision");

        assertEquals("provider-vision", LlmClientImpl.resolveVisionModel(null, provider));

        provider.setVisionModel(null);
        assertEquals("provider-default", LlmClientImpl.resolveVisionModel(null, provider));
    }
}
