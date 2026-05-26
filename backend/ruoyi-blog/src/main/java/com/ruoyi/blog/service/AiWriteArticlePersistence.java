package com.ruoyi.blog.service;

import com.ruoyi.blog.dto.AiWriteWizardRequest;

public interface AiWriteArticlePersistence
{

    Long saveGeneratedDraft(AiWriteWizardRequest request, String content);
}
