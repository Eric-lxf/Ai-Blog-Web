package com.ruoyi.blog.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruoyi.blog.config.DeepSeekProperties;
import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.dto.ChatMessageDTO;
import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.service.AiPromptTemplateService;
import com.ruoyi.blog.service.DeepSeekService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Service
public class DeepSeekServiceImpl implements DeepSeekService
{

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final DeepSeekProperties deepSeekProperties;

    private final AiPromptTemplateService aiPromptTemplateService;

    private final ObjectMapper objectMapper;

    private final OkHttpClient deepSeekOkHttpClient;

    private final Executor aiTaskExecutor;

    @Autowired
    public DeepSeekServiceImpl(DeepSeekProperties deepSeekProperties,
            AiPromptTemplateService aiPromptTemplateService,
            ObjectMapper objectMapper,
            OkHttpClient deepSeekOkHttpClient,
            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor)
    {
        this.deepSeekProperties = deepSeekProperties;
        this.aiPromptTemplateService = aiPromptTemplateService;
        this.objectMapper = objectMapper;
        this.deepSeekOkHttpClient = deepSeekOkHttpClient;
        this.aiTaskExecutor = aiTaskExecutor;
    }

    @Override
    public String chatCompletion(AiCompletionRequest request)
    {
        if (!deepSeekProperties.isConfigured())
        {
            throw new ServiceException("未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY", HttpStatus.ERROR);
        }
        try
        {
            AiPromptTemplate template = aiPromptTemplateService.getByScene(
                    StringUtils.hasText(request.getScene()) ? request.getScene() : "CHAT");
            if (template == null)
            {
                throw new ServiceException("未找到可用的 AI 提示词模板", HttpStatus.ERROR);
            }
            String requestBody = buildCompletionRequestBody(request, template);
            Request httpRequest = new Request.Builder().url(deepSeekProperties.getBaseUrl() + "/v1/chat/completions")
                    .header("Authorization", "Bearer " + deepSeekProperties.getApiKey()).header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON)).build();
            try (Response response = deepSeekOkHttpClient.newCall(httpRequest).execute())
            {
                if (!response.isSuccessful())
                {
                    log.warn("DeepSeek HTTP error: {}", response.code());
                    throw new ServiceException("AI 服务暂时不可用，请稍后重试", HttpStatus.ERROR);
                }
                ResponseBody body = response.body();
                if (body == null)
                {
                    throw new ServiceException("AI 服务返回为空", HttpStatus.ERROR);
                }
                JsonNode node = objectMapper.readTree(body.string());
                return node.path("choices").get(0).path("message").path("content").asText("");
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("DeepSeek completion error", e);
            throw new ServiceException("AI 服务异常，请稍后重试", HttpStatus.ERROR);
        }
    }

    @Override
    public void streamChat(AiChatRequest request, SseEmitter emitter)
    {
        if (!deepSeekProperties.isConfigured())
        {
            sendErrorAndComplete(emitter, "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        aiTaskExecutor.execute(() -> {
            try
            {
                AiPromptTemplate template = aiPromptTemplateService.getByScene(
                        StringUtils.hasText(request.getScene()) ? request.getScene() : "CHAT");
                if (template == null)
                {
                    sendErrorAndComplete(emitter, "未找到可用的 AI 提示词模板");
                    return;
                }

                String requestBody = buildRequestBody(request, template);
                Request httpRequest = new Request.Builder().url(deepSeekProperties.getBaseUrl() + "/v1/chat/completions")
                        .header("Authorization", "Bearer " + deepSeekProperties.getApiKey()).header("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody, JSON)).build();

                try (Response response = deepSeekOkHttpClient.newCall(httpRequest).execute())
                {
                    if (!response.isSuccessful())
                    {
                        log.warn("DeepSeek stream HTTP error: {}", response.code());
                        sendErrorAndComplete(emitter, "AI 服务暂时不可用，请稍后重试");
                        return;
                    }
                    ResponseBody body = response.body();
                    if (body == null)
                    {
                        sendErrorAndComplete(emitter, "AI 服务返回为空");
                        return;
                    }
                    streamResponse(body, emitter);
                }
                emitter.send("[DONE]");
                emitter.complete();
            }
            catch (Exception e)
            {
                log.error("DeepSeek stream error", e);
                try
                {
                    sendErrorAndComplete(emitter, "AI 服务异常，请稍后重试");
                }
                catch (Exception ignored)
                {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    private String buildCompletionRequestBody(AiCompletionRequest request, AiPromptTemplate template) throws Exception
    {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model",
                StringUtils.hasText(template.getModelName()) ? template.getModelName() : deepSeekProperties.getModel());
        root.put("stream", false);
        applyTemperature(root, request.getTemperature(), template.getTemperature());
        ArrayNode messages = root.putArray("messages");
        String system = StringUtils.hasText(request.getCustomSystemPrompt())
                ? request.getCustomSystemPrompt() : template.getSystemPrompt();
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", system));
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", request.getPrompt()));
        return objectMapper.writeValueAsString(root);
    }

    private String buildRequestBody(AiChatRequest request, AiPromptTemplate template) throws Exception
    {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model",
                StringUtils.hasText(template.getModelName()) ? template.getModelName() : deepSeekProperties.getModel());
        root.put("stream", true);
        applyTemperature(root, null, template.getTemperature());
        ArrayNode messages = root.putArray("messages");
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", template.getSystemPrompt()));

        if (Boolean.TRUE.equals(request.getIncludeContext()))
        {
            String context = buildArticleContext(request);
            if (StringUtils.hasText(context))
            {
                messages.add(objectMapper.createObjectNode().put("role", "system").put("content", "当前用户正在编辑的博客文章上下文：\n" + context));
            }
        }

        if (!CollectionUtils.isEmpty(request.getHistory()))
        {
            for (ChatMessageDTO msg : request.getHistory())
            {
                messages.add(objectMapper.createObjectNode().put("role", msg.getRole()).put("content", msg.getContent()));
            }
        }

        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", request.getPrompt()));

        return objectMapper.writeValueAsString(root);
    }

    private String buildArticleContext(AiChatRequest request)
    {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.getArticleTitle()))
        {
            sb.append("标题：").append(request.getArticleTitle()).append("\n");
        }
        if (StringUtils.hasText(request.getArticleContent()))
        {
            String content = request.getArticleContent();
            if (content.length() > 12000)
            {
                content = content.substring(0, 12000) + "\n...(内容已截断)";
            }
            sb.append("正文：\n").append(content);
        }
        return sb.toString().trim();
    }

    private void streamResponse(ResponseBody body, SseEmitter emitter) throws Exception
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (!line.startsWith("data: "))
                {
                    continue;
                }
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data))
                {
                    break;
                }
                JsonNode node = objectMapper.readTree(data);
                JsonNode choices = node.path("choices");
                if (!choices.isArray() || choices.isEmpty())
                {
                    continue;
                }
                String content = choices.get(0).path("delta").path("content").asText("");
                if (StringUtils.hasText(content))
                {
                    emitter.send(content);
                }
            }
        }
    }

    private void applyTemperature(ObjectNode root, java.math.BigDecimal override, java.math.BigDecimal templateTemp)
    {
        java.math.BigDecimal temperature = override != null ? override : templateTemp;
        if (temperature != null)
        {
            root.put("temperature", temperature.doubleValue());
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message)
    {
        try
        {
            emitter.send(SseEmitter.event().name("error").data(message));
        }
        catch (Exception e)
        {
            log.warn("Failed to send SSE error", e);
        }
        emitter.complete();
    }
}
