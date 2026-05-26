package com.ruoyi.blog.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.mapper.AiPromptTemplateMapper;
import com.ruoyi.blog.service.AiPromptTemplateService;
import com.ruoyi.blog.vo.AiPromptTemplateVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiPromptTemplateServiceImpl implements AiPromptTemplateService
{

    private final AiPromptTemplateMapper aiPromptTemplateMapper;

    @Override
    public List<AiPromptTemplateVO> listActive()
    {
        return aiPromptTemplateMapper
                .selectList(new LambdaQueryWrapper<AiPromptTemplate>().eq(AiPromptTemplate::getIsActive, 1).orderByAsc(AiPromptTemplate::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public AiPromptTemplate getByScene(String sceneType)
    {
        AiPromptTemplate template = aiPromptTemplateMapper.selectOne(
                new LambdaQueryWrapper<AiPromptTemplate>().eq(AiPromptTemplate::getSceneType, sceneType).eq(AiPromptTemplate::getIsActive, 1)
                        .last("LIMIT 1"));
        if (template == null)
        {
            template = aiPromptTemplateMapper.selectOne(new LambdaQueryWrapper<AiPromptTemplate>()
                    .eq(AiPromptTemplate::getSceneType, "CHAT").eq(AiPromptTemplate::getIsActive, 1).last("LIMIT 1"));
        }
        return template;
    }

    private AiPromptTemplateVO toVO(AiPromptTemplate entity)
    {
        AiPromptTemplateVO vo = new AiPromptTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
