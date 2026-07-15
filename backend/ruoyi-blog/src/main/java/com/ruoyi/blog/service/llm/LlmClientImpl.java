package com.ruoyi.blog.service.llm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.blog.constant.AiProviderType;
import com.ruoyi.blog.domain.AiProvider;
import com.ruoyi.blog.domain.AiPromptTemplate;
import com.ruoyi.blog.dto.AiChatRequest;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.dto.ChatMessageDTO;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClientImpl implements LlmClient
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String ANTHROPIC_OAUTH_BETA = "oauth-2025-04-20";

    private final ObjectMapper objectMapper;

    @Override
    public String chatCompletion(AiProvider provider, AiCompletionRequest request, AiPromptTemplate template,
            String textModel, BigDecimal effectiveTemperature, OkHttpClient client)
    {
        try
        {
            if (AiProviderType.isAnthropic(provider.getProviderType()))
            {
                return anthropicCompletion(provider, request, template, textModel, effectiveTemperature, client);
            }
            return openAiCompletion(provider, request, template, textModel, effectiveTemperature, client);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("LLM completion error provider={}", provider.getName(), e);
            throw new ServiceException("AI 服务异常，请稍后重试", HttpStatus.ERROR);
        }
    }

    @Override
    public void streamChat(AiProvider provider, AiChatRequest request, AiPromptTemplate template, String textModel,
            BigDecimal effectiveTemperature, OkHttpClient client, SseEmitter emitter) throws Exception
    {
        if (AiProviderType.isAnthropic(provider.getProviderType()))
        {
            anthropicStream(provider, request, template, textModel, effectiveTemperature, client, emitter);
            return;
        }
        openAiStream(provider, request, template, textModel, effectiveTemperature, client, emitter);
    }

    @Override
    public String recognizeImage(AiProvider provider, String imageUrl, String textPrompt, String visionModel,
            OkHttpClient client)
    {
        if (AiProviderType.isAnthropic(provider.getProviderType()))
        {
            return anthropicVision(provider, imageUrl, textPrompt, visionModel, client);
        }
        return openAiVision(provider, imageUrl, textPrompt, visionModel, client);
    }

    @Override
    public void testConnection(AiProvider provider, OkHttpClient client)
    {
        AiCompletionRequest request = new AiCompletionRequest();
        request.setScene("CHAT");
        request.setPrompt("ping");
        request.setCustomSystemPrompt("Reply with exactly: ok");
        AiPromptTemplate template = new AiPromptTemplate();
        template.setSystemPrompt("Reply with exactly: ok");
        String result = chatCompletion(provider, request, template, provider.getDefaultModel(), request.getTemperature(),
                client);
        if (!StringUtils.hasText(result))
        {
            throw new ServiceException("AI 返回为空，请检查 Key 与模型配置", HttpStatus.ERROR);
        }
    }

    // -------------------- OpenAI Compatible --------------------

    private String openAiCompletion(AiProvider provider, AiCompletionRequest request, AiPromptTemplate template,
            String textModel, BigDecimal effectiveTemperature, OkHttpClient client) throws Exception
    {
        String body = buildOpenAiCompletionBody(provider, request, template, textModel, effectiveTemperature, false);
        Request httpRequest = openAiRequest(provider, body);
        try (Response response = client.newCall(httpRequest).execute())
        {
            assertSuccess(response, "OpenAI");
            JsonNode node = objectMapper.readTree(response.body().string());
            return node.path("choices").get(0).path("message").path("content").asText("");
        }
    }

    private void openAiStream(AiProvider provider, AiChatRequest request, AiPromptTemplate template, String textModel,
            BigDecimal effectiveTemperature, OkHttpClient client, SseEmitter emitter) throws Exception
    {
        String body = buildOpenAiChatBody(provider, request, template, textModel, effectiveTemperature);
        Request httpRequest = openAiRequest(provider, body);
        try (Response response = client.newCall(httpRequest).execute())
        {
            if (!response.isSuccessful())
            {
                log.warn("OpenAI stream HTTP error: {}", response.code());
                throw new ServiceException("AI 服务暂时不可用，请稍后重试", HttpStatus.ERROR);
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                throw new ServiceException("AI 服务返回为空", HttpStatus.ERROR);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8)))
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
    }

    private String openAiVision(AiProvider provider, String imageUrl, String textPrompt, String visionModel,
            OkHttpClient client)
    {
        try
        {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", resolveVisionModel(visionModel, provider));
            root.put("stream", false);
            ArrayNode messages = root.putArray("messages");
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            ArrayNode content = userMsg.putArray("content");
            ObjectNode imgPart = objectMapper.createObjectNode();
            imgPart.put("type", "image_url");
            ObjectNode imgUrlNode = objectMapper.createObjectNode();
            imgUrlNode.put("url", imageUrl);
            imgPart.set("image_url", imgUrlNode);
            content.add(imgPart);
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "text");
            textPart.put("text", textPrompt);
            content.add(textPart);
            messages.add(userMsg);

            Request httpRequest = openAiRequest(provider, objectMapper.writeValueAsString(root));
            try (Response response = client.newCall(httpRequest).execute())
            {
                assertSuccess(response, "OpenAI vision");
                JsonNode node = objectMapper.readTree(response.body().string());
                return node.path("choices").get(0).path("message").path("content").asText("");
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("OpenAI vision error", e);
            throw new ServiceException("AI 识别服务异常，请稍后重试", HttpStatus.ERROR);
        }
    }

    private Request openAiRequest(AiProvider provider, String body)
    {
        return new Request.Builder().url(provider.getBaseUrl() + "/v1/chat/completions")
                .header("Authorization", "Bearer " + provider.getApiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON))
                .build();
    }

    private String buildOpenAiCompletionBody(AiProvider provider, AiCompletionRequest request, AiPromptTemplate template,
            String textModel, BigDecimal effectiveTemperature, boolean stream) throws Exception
    {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", resolveTextModel(textModel, provider, template));
        root.put("stream", stream);
        applyTemperature(root, effectiveTemperature);
        ArrayNode messages = root.putArray("messages");
        String system = StringUtils.hasText(request.getCustomSystemPrompt()) ? request.getCustomSystemPrompt()
                : template.getSystemPrompt();
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", system));
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", request.getPrompt()));
        return objectMapper.writeValueAsString(root);
    }

    private String buildOpenAiChatBody(AiProvider provider, AiChatRequest request, AiPromptTemplate template, String textModel,
            BigDecimal effectiveTemperature) throws Exception
    {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", resolveTextModel(textModel, provider, template));
        root.put("stream", true);
        applyTemperature(root, effectiveTemperature);
        ArrayNode messages = root.putArray("messages");
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", template.getSystemPrompt()));
        if (Boolean.TRUE.equals(request.getIncludeContext()))
        {
            String context = buildArticleContext(request);
            if (StringUtils.hasText(context))
            {
                messages.add(objectMapper.createObjectNode().put("role", "system")
                        .put("content", "当前用户正在编辑的博客文章上下文：\n" + context));
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

    // -------------------- Anthropic Claude --------------------

    private String anthropicCompletion(AiProvider provider, AiCompletionRequest request, AiPromptTemplate template,
            String textModel, BigDecimal effectiveTemperature, OkHttpClient client) throws Exception
    {
        String system = StringUtils.hasText(request.getCustomSystemPrompt()) ? request.getCustomSystemPrompt()
                : template.getSystemPrompt();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", resolveTextModel(textModel, provider, template));
        root.put("max_tokens", 4096);
        root.put("stream", false);
        if (StringUtils.hasText(system))
        {
            root.put("system", system);
        }
        applyTemperature(root, effectiveTemperature);
        ArrayNode messages = root.putArray("messages");
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", request.getPrompt()));

        Request httpRequest = anthropicRequest(provider, objectMapper.writeValueAsString(root));
        try (Response response = client.newCall(httpRequest).execute())
        {
            assertSuccess(response, "Anthropic");
            JsonNode node = objectMapper.readTree(response.body().string());
            return extractAnthropicText(node);
        }
    }

    private void anthropicStream(AiProvider provider, AiChatRequest request, AiPromptTemplate template,
            String textModel, BigDecimal effectiveTemperature, OkHttpClient client, SseEmitter emitter) throws Exception
    {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", resolveTextModel(textModel, provider, template));
        root.put("max_tokens", 4096);
        root.put("stream", true);
        StringBuilder system = new StringBuilder();
        if (StringUtils.hasText(template.getSystemPrompt()))
        {
            system.append(template.getSystemPrompt());
        }
        if (Boolean.TRUE.equals(request.getIncludeContext()))
        {
            String context = buildArticleContext(request);
            if (StringUtils.hasText(context))
            {
                if (system.length() > 0)
                {
                    system.append("\n\n");
                }
                system.append("当前用户正在编辑的博客文章上下文：\n").append(context);
            }
        }
        if (system.length() > 0)
        {
            root.put("system", system.toString());
        }
        applyTemperature(root, effectiveTemperature);
        ArrayNode messages = root.putArray("messages");
        if (!CollectionUtils.isEmpty(request.getHistory()))
        {
            for (ChatMessageDTO msg : request.getHistory())
            {
                String role = "assistant".equalsIgnoreCase(msg.getRole()) ? "assistant" : "user";
                messages.add(objectMapper.createObjectNode().put("role", role).put("content", msg.getContent()));
            }
        }
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", request.getPrompt()));

        Request httpRequest = anthropicRequest(provider, objectMapper.writeValueAsString(root));
        try (Response response = client.newCall(httpRequest).execute())
        {
            if (!response.isSuccessful())
            {
                log.warn("Anthropic stream HTTP error: {}", response.code());
                throw new ServiceException("AI 服务暂时不可用，请稍后重试", HttpStatus.ERROR);
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null)
            {
                throw new ServiceException("AI 服务返回为空", HttpStatus.ERROR);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (!line.startsWith("data: "))
                    {
                        continue;
                    }
                    String data = line.substring(6).trim();
                    if (!StringUtils.hasText(data))
                    {
                        continue;
                    }
                    JsonNode node = objectMapper.readTree(data);
                    String type = node.path("type").asText("");
                    if ("content_block_delta".equals(type))
                    {
                        String text = node.path("delta").path("text").asText("");
                        if (StringUtils.hasText(text))
                        {
                            emitter.send(text);
                        }
                    }
                    else if ("message_stop".equals(type))
                    {
                        break;
                    }
                }
            }
        }
    }

    private String anthropicVision(AiProvider provider, String imageUrl, String textPrompt, String visionModel,
            OkHttpClient client)
    {
        try
        {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", resolveVisionModel(visionModel, provider));
            root.put("max_tokens", 2048);
            ArrayNode messages = root.putArray("messages");
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            ArrayNode content = userMsg.putArray("content");

            ObjectNode imgPart = objectMapper.createObjectNode();
            imgPart.put("type", "image");
            ObjectNode source = objectMapper.createObjectNode();
            if (imageUrl.startsWith("data:"))
            {
                int comma = imageUrl.indexOf(',');
                String meta = imageUrl.substring(5, comma);
                String mediaType = meta.contains(";") ? meta.substring(0, meta.indexOf(';')) : "image/jpeg";
                source.put("type", "base64");
                source.put("media_type", mediaType);
                source.put("data", imageUrl.substring(comma + 1));
            }
            else
            {
                source.put("type", "url");
                source.put("url", imageUrl);
            }
            imgPart.set("source", source);
            content.add(imgPart);

            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "text");
            textPart.put("text", textPrompt);
            content.add(textPart);
            messages.add(userMsg);

            Request httpRequest = anthropicRequest(provider, objectMapper.writeValueAsString(root));
            try (Response response = client.newCall(httpRequest).execute())
            {
                assertSuccess(response, "Anthropic vision");
                return extractAnthropicText(objectMapper.readTree(response.body().string()));
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            log.error("Anthropic vision error", e);
            throw new ServiceException("AI 识别服务异常，请稍后重试", HttpStatus.ERROR);
        }
    }

    private Request anthropicRequest(AiProvider provider, String body)
    {
        Request.Builder builder = new Request.Builder().url(provider.getBaseUrl() + "/v1/messages")
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json");
        if (AiProviderType.AUTH_MODE_AUTH_TOKEN.equalsIgnoreCase(provider.getAuthMode()))
        {
            builder.header("Authorization", "Bearer " + provider.getApiKey())
                    .header("anthropic-beta", ANTHROPIC_OAUTH_BETA);
        }
        else
        {
            builder.header("x-api-key", provider.getApiKey());
        }
        return builder.post(RequestBody.create(body, JSON)).build();
    }

    private String extractAnthropicText(JsonNode node)
    {
        JsonNode content = node.path("content");
        if (!content.isArray() || content.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content)
        {
            if ("text".equals(block.path("type").asText()))
            {
                sb.append(block.path("text").asText(""));
            }
        }
        return sb.toString();
    }

    // -------------------- Shared --------------------

    static String resolveTextModel(String resolvedTextModel, AiProvider provider, AiPromptTemplate ignoredTemplate)
    {
        if (StringUtils.hasText(resolvedTextModel))
        {
            return resolvedTextModel;
        }
        return provider.getDefaultModel();
    }

    static String resolveVisionModel(String resolvedVisionModel, AiProvider provider)
    {
        if (StringUtils.hasText(resolvedVisionModel))
        {
            return resolvedVisionModel;
        }
        if (StringUtils.hasText(provider.getVisionModel()))
        {
            return provider.getVisionModel();
        }
        return provider.getDefaultModel();
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

    private void applyTemperature(ObjectNode root, BigDecimal effectiveTemperature)
    {
        if (effectiveTemperature != null)
        {
            root.put("temperature", effectiveTemperature.doubleValue());
        }
    }

    private void assertSuccess(Response response, String label) throws Exception
    {
        if (!response.isSuccessful())
        {
            String errBody = response.body() != null ? response.body().string() : "";
            log.warn("{} HTTP error: {} body={}", label, response.code(),
                    errBody.length() > 300 ? errBody.substring(0, 300) : errBody);
            throw new ServiceException("AI 服务暂时不可用，请稍后重试", HttpStatus.ERROR);
        }
        if (response.body() == null)
        {
            throw new ServiceException("AI 服务返回为空", HttpStatus.ERROR);
        }
    }
}
