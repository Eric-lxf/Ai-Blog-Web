package com.ruoyi.blog.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.ruoyi.blog.domain.AiTaskRecord;
import com.ruoyi.blog.mapper.AiTaskRecordMapper;
import com.ruoyi.blog.service.AiTaskService;
import com.ruoyi.blog.vo.AiTaskVO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiTaskServiceImpl implements AiTaskService
{

    private final AiTaskRecordMapper aiTaskRecordMapper;

    @Override
    public AiTaskVO getById(Long id)
    {
        AiTaskRecord record = aiTaskRecordMapper.selectById(id);
        if (record == null)
        {
            throw new ServiceException("资源不存在", HttpStatus.NOT_FOUND);
        }
        AiTaskVO vo = new AiTaskVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    @Override
    public Long createTask(String taskType, String promptPayload)
    {
        AiTaskRecord record = new AiTaskRecord();
        record.setTaskType(taskType);
        record.setPromptPayload(promptPayload);
        record.setStatus(0);
        record.setTokensUsed(0);
        aiTaskRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public void markRunning(Long taskId)
    {
        AiTaskRecord record = new AiTaskRecord();
        record.setId(taskId);
        record.setStatus(1);
        aiTaskRecordMapper.updateById(record);
    }

    @Override
    public void markSuccess(Long taskId, Long articleId, String resultContent)
    {
        AiTaskRecord record = new AiTaskRecord();
        record.setId(taskId);
        record.setStatus(2);
        record.setTargetArticleId(articleId);
        record.setResultContent(resultContent);
        record.setFinishTime(LocalDateTime.now());
        aiTaskRecordMapper.updateById(record);
    }

    @Override
    public void markFailed(Long taskId, String errorMessage)
    {
        AiTaskRecord record = new AiTaskRecord();
        record.setId(taskId);
        record.setStatus(3);
        record.setErrorMessage(errorMessage);
        record.setFinishTime(LocalDateTime.now());
        aiTaskRecordMapper.updateById(record);
    }

    @Override
    public void saveIntermediate(Long taskId, String intermediateJson)
    {
        AiTaskRecord record = new AiTaskRecord();
        record.setId(taskId);
        record.setIntermediateData(intermediateJson);
        aiTaskRecordMapper.updateById(record);
    }
}
