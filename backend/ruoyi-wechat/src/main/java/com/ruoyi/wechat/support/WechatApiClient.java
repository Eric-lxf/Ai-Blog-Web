package com.ruoyi.wechat.support;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.wechat.config.WechatProperties;

import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@RequiredArgsConstructor
public class WechatApiClient
{
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final WechatProperties properties;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getJson(String url)
    {
        OkHttpClient client = buildClient();
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new ServiceException("wechat api call failed: " + response.code());
            }
            String body = response.body() == null ? "{}" : response.body().string();
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>()
            {
            });
        }
        catch (IOException e)
        {
            throw new ServiceException("wechat api call exception: " + e.getMessage());
        }
    }

    public Map<String, Object> postJson(String url, Object payload)
    {
        OkHttpClient client = buildClient();
        try
        {
            String bodyJson = objectMapper.writeValueAsString(payload);
            Request request = new Request.Builder().url(url).post(RequestBody.create(bodyJson, JSON)).build();
            try (Response response = client.newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    throw new ServiceException("wechat api call failed: " + response.code());
                }
                String body = response.body() == null ? "{}" : response.body().string();
                return objectMapper.readValue(body, new TypeReference<Map<String, Object>>()
                {
                });
            }
        }
        catch (IOException e)
        {
            throw new ServiceException("wechat api call exception: " + e.getMessage());
        }
    }

    public Map<String, Object> postMultipart(String url, String fieldName, String filename, byte[] bytes, String mediaType)
    {
        OkHttpClient client = buildClient();
        RequestBody fileBody = RequestBody.create(bytes, MediaType.parse(mediaType));
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(fieldName, filename, fileBody)
                .build();
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new ServiceException("wechat api call failed: " + response.code());
            }
            String respBody = response.body() == null ? "{}" : response.body().string();
            return objectMapper.readValue(respBody, new TypeReference<Map<String, Object>>()
            {
            });
        }
        catch (IOException e)
        {
            throw new ServiceException("wechat api call exception: " + e.getMessage());
        }
    }

    private OkHttpClient buildClient()
    {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }
}
