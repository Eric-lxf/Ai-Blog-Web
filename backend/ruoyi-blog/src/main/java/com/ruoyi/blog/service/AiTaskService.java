package com.ruoyi.blog.service;

import com.ruoyi.blog.vo.AiTaskVO;

public interface AiTaskService
{

    AiTaskVO getById(Long id);

    Long createTask(String taskType, String promptPayload);

    void markRunning(Long taskId);

    void markSuccess(Long taskId, Long articleId, String resultContent);

    void markFailed(Long taskId, String errorMessage);

    void saveIntermediate(Long taskId, String intermediateJson);
}
