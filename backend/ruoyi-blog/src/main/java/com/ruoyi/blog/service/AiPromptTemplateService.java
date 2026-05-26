package com.ruoyi.blog.service;

import java.util.List;

import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.vo.AiPromptTemplateVO;

public interface AiPromptTemplateService
{

    List<AiPromptTemplateVO> listActive();

    AiPromptTemplate getByScene(String sceneType);
}
