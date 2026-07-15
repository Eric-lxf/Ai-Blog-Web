package com.ruoyi.blog.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.dto.AiWriteWizardRequest;
import com.ruoyi.blog.dto.OutlineNodeDTO;
import com.ruoyi.blog.service.AiTaskService;
import com.ruoyi.blog.service.AiWriteArticlePersistence;
import com.ruoyi.blog.service.AiWriteService;
import com.ruoyi.blog.service.DeepSeekService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiWriteServiceImpl implements AiWriteService
{

    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*]", Pattern.DOTALL);

    private final DeepSeekService deepSeekService;

    private final AiTaskService aiTaskService;

    private final AiWriteArticlePersistence aiWriteArticlePersistence;

    private final ObjectMapper objectMapper;

    private final Executor aiTaskExecutor;

    @Autowired
    public AiWriteServiceImpl(DeepSeekService deepSeekService,
            AiTaskService aiTaskService,
            AiWriteArticlePersistence aiWriteArticlePersistence,
            ObjectMapper objectMapper,
            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor)
    {
        this.deepSeekService = deepSeekService;
        this.aiTaskService = aiTaskService;
        this.aiWriteArticlePersistence = aiWriteArticlePersistence;
        this.objectMapper = objectMapper;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @Override
    public List<String> generateTitles(AiWriteWizardRequest request)
    {
        String prompt = """
                技术主题：%s
                目标读者：%s
                目标篇幅：%s
                请生成 5 个适合技术博客的标题，以 JSON 字符串数组返回，例如：["标题1","标题2"]
                只输出 JSON，不要其他文字。
                """.formatted(request.getTopic(), audienceLabel(request.getAudience()), lengthLabel(request.getLength()));
        String raw = deepSeekService.chatCompletion(completion("TITLE_GEN", prompt), AiModuleCode.WRITE);
        return parseStringList(raw);
    }

    @Override
    public String generateSummary(AiWriteWizardRequest request)
    {
        String prompt = """
                技术主题：%s
                文章标题：%s
                目标读者：%s
                请写一段 80-150 字的中文摘要，适合作为博客 SEO 描述。只输出摘要正文。
                """.formatted(request.getTopic(), request.getTitle(), audienceLabel(request.getAudience()));
        return deepSeekService.chatCompletion(completion("SUMMARY", prompt), AiModuleCode.WRITE).trim();
    }

    @Override
    public List<OutlineNodeDTO> generateOutline(AiWriteWizardRequest request)
    {
        String prompt = """
                技术主题：%s
                文章标题：%s
                摘要：%s
                目标读者：%s
                篇幅：%s
                请生成文章大纲，以 JSON 数组返回。每个节点格式：
                {"id":"1","title":"章节标题","children":[{"id":"1-1","title":"小节"}]}
                至少 3 个一级章节，技术博客结构清晰。只输出 JSON。
                """.formatted(request.getTopic(), request.getTitle(), request.getSummary(),
                audienceLabel(request.getAudience()), lengthLabel(request.getLength()));
        String raw = deepSeekService.chatCompletion(completion("OUTLINE_GEN", prompt), AiModuleCode.WRITE);
        return parseOutline(raw);
    }

    @Override
    public Long submitGenerateArticle(AiWriteWizardRequest request)
    {
        Long taskId;
        try
        {
            taskId = aiTaskService.createTask("GENERATE_ARTICLE", objectMapper.writeValueAsString(request));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        try
        {
            aiTaskService.saveIntermediate(taskId, serializeOutline(request.getOutline()));
        }
        catch (Exception e)
        {
            log.warn("Save outline intermediate failed", e);
        }
        aiTaskExecutor.execute(() -> runGenerateTask(taskId, request));
        return taskId;
    }

    private void runGenerateTask(Long taskId, AiWriteWizardRequest request)
    {
        aiTaskService.markRunning(taskId);
        try
        {
            String outlineText = outlineToMarkdown(request.getOutline());
            String prompt = """
                    请根据以下信息撰写一篇完整的技术博客（Markdown）：
                    标题：%s
                    主题：%s
                    摘要：%s
                    大纲：
                    %s
                    要求：包含代码示例与必要的 ```mermaid 图表；段落清晰；不要输出标题以外的多余说明。
                    """.formatted(request.getTitle(), request.getTopic(), request.getSummary(), outlineText);
            String content = deepSeekService.chatCompletion(completion("FULL_ARTICLE", prompt), AiModuleCode.WRITE).trim();
            Long articleId = aiWriteArticlePersistence.saveGeneratedDraft(request, content);
            aiTaskService.markSuccess(taskId, articleId, content);
        }
        catch (Exception e)
        {
            log.error("Generate article task failed, taskId={}", taskId, e);
            aiTaskService.markFailed(taskId, e.getMessage());
        }
    }

    private AiCompletionRequest completion(String scene, String prompt)
    {
        AiCompletionRequest req = new AiCompletionRequest();
        req.setScene(scene);
        req.setPrompt(prompt);
        return req;
    }

    private String audienceLabel(String audience)
    {
        return switch (audience == null ? "mid" : audience)
        {
            case "novice" -> "新手";
            case "junior" -> "1-3年";
            case "senior" -> "5年以上";
            default -> "3-5年";
        };
    }

    private String lengthLabel(String length)
    {
        return switch (length == null ? "medium" : length)
        {
            case "short" -> "简洁";
            case "long" -> "专业长文";
            default -> "标准";
        };
    }

    private List<String> parseStringList(String raw)
    {
        try
        {
            String json = extractJson(raw);
            return objectMapper.readValue(json, new TypeReference<List<String>>()
            {
            });
        }
        catch (Exception e)
        {
            return List.of(raw.trim());
        }
    }

    private List<OutlineNodeDTO> parseOutline(String raw)
    {
        try
        {
            String json = extractJson(raw);
            return objectMapper.readValue(json, new TypeReference<List<OutlineNodeDTO>>()
            {
            });
        }
        catch (Exception e)
        {
            OutlineNodeDTO node = new OutlineNodeDTO();
            node.setId("1");
            node.setTitle("正文");
            node.setChildren(new ArrayList<>());
            return List.of(node);
        }
    }

    private String extractJson(String raw)
    {
        Matcher m = JSON_ARRAY.matcher(raw);
        if (m.find())
        {
            return m.group();
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start)
        {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String serializeOutline(List<OutlineNodeDTO> outline) throws Exception
    {
        return objectMapper.writeValueAsString(outline);
    }

    private String outlineToMarkdown(List<OutlineNodeDTO> nodes)
    {
        StringBuilder sb = new StringBuilder();
        appendOutline(sb, nodes, 1);
        return sb.toString();
    }

    private void appendOutline(StringBuilder sb, List<OutlineNodeDTO> nodes, int level)
    {
        if (CollectionUtils.isEmpty(nodes))
        {
            return;
        }
        for (OutlineNodeDTO node : nodes)
        {
            sb.append("#".repeat(Math.min(level, 6))).append(" ").append(node.getTitle()).append("\n");
            appendOutline(sb, node.getChildren(), level + 1);
        }
    }
}
