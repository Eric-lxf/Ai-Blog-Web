package com.ruoyi.blog.service;

import java.util.List;

import com.ruoyi.blog.dto.AiWriteWizardRequest;
import com.ruoyi.blog.dto.OutlineNodeDTO;

public interface AiWriteService
{

    List<String> generateTitles(AiWriteWizardRequest request);

    String generateSummary(AiWriteWizardRequest request);

    List<OutlineNodeDTO> generateOutline(AiWriteWizardRequest request);

    Long submitGenerateArticle(AiWriteWizardRequest request);
}
