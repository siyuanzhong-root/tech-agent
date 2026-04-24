package com.edu.agent.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class DeepSeekService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    
    @Value("${deepseek.api.key:your-deepseek-api-key}")
    private String apiKey;
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public DeepSeekService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    public String chat(String userMessage) {
        logDeepSeekCall("CHAT", userMessage, null);
        
        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", userMessage);
            
            JsonArray messages = new JsonArray();
            messages.add(message);
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "deepseek-chat");
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2000);
            
            String requestBodyStr = requestBody.toString();
            logger.info("[DeepSeek] 请求体: {}", requestBodyStr);
            
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBodyStr
            );
            
            Request request = new Request.Builder()
                    .url(DEEPSEEK_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            long startTime = System.currentTimeMillis();
            
            try (Response response = client.newCall(request).execute()) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                logger.info("[DeepSeek] API响应时间: {} ms", duration);
                
                if (!response.isSuccessful()) {
                    String errorMsg = "API调用失败: " + response.code() + " - " + response.message();
                    logger.error("[DeepSeek] {}", errorMsg);
                    return errorMsg;
                }
                
                String responseBody = response.body().string();
                logger.info("[DeepSeek] 响应体: {}", responseBody);
                
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    JsonObject messageObj = firstChoice.getAsJsonObject("message");
                    String content = messageObj.get("content").getAsString();
                    
                    // 记录token使用情况
                    JsonObject usage = jsonResponse.getAsJsonObject("usage");
                    if (usage != null) {
                        int promptTokens = usage.get("prompt_tokens").getAsInt();
                        int completionTokens = usage.get("completion_tokens").getAsInt();
                        int totalTokens = usage.get("total_tokens").getAsInt();
                        logger.info("[DeepSeek] Token使用 - 输入: {}, 输出: {}, 总计: {}", 
                            promptTokens, completionTokens, totalTokens);
                    }
                    
                    logDeepSeekResponse(content, duration);
                    return content;
                }
                
                return "无法解析API响应";
            }
        } catch (IOException e) {
            logger.error("[DeepSeek] API调用出错: {}", e.getMessage(), e);
            return "API调用出错: " + e.getMessage();
        }
    }
    
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        logDeepSeekCall("CHAT_WITH_SYSTEM", userMessage, systemPrompt);
        
        try {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            
            JsonArray messages = new JsonArray();
            messages.add(systemMessage);
            messages.add(userMsg);
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "deepseek-chat");
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2000);
            
            String requestBodyStr = requestBody.toString();
            logger.info("[DeepSeek] 请求体: {}", requestBodyStr);
            
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestBodyStr
            );
            
            Request request = new Request.Builder()
                    .url(DEEPSEEK_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            long startTime = System.currentTimeMillis();
            
            try (Response response = client.newCall(request).execute()) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                logger.info("[DeepSeek] API响应时间: {} ms", duration);
                
                if (!response.isSuccessful()) {
                    String errorMsg = "API调用失败: " + response.code() + " - " + response.message();
                    logger.error("[DeepSeek] {}", errorMsg);
                    return errorMsg;
                }
                
                String responseBody = response.body().string();
                logger.info("[DeepSeek] 响应体: {}", responseBody);
                
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject firstChoice = choices.get(0).getAsJsonObject();
                    JsonObject messageObj = firstChoice.getAsJsonObject("message");
                    String content = messageObj.get("content").getAsString();
                    
                    // 记录token使用情况
                    JsonObject usage = jsonResponse.getAsJsonObject("usage");
                    if (usage != null) {
                        int promptTokens = usage.get("prompt_tokens").getAsInt();
                        int completionTokens = usage.get("completion_tokens").getAsInt();
                        int totalTokens = usage.get("total_tokens").getAsInt();
                        logger.info("[DeepSeek] Token使用 - 输入: {}, 输出: {}, 总计: {}", 
                            promptTokens, completionTokens, totalTokens);
                    }
                    
                    logDeepSeekResponse(content, duration);
                    return content;
                }
                
                return "无法解析API响应";
            }
        } catch (IOException e) {
            logger.error("[DeepSeek] API调用出错: {}", e.getMessage(), e);
            return "API调用出错: " + e.getMessage();
        }
    }
    
    private void logDeepSeekCall(String callType, String userMessage, String systemPrompt) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║           DeepSeek API 调用 - {}", timestamp);
        logger.info("╠══════════════════════════════════════════════════════════════╣");
        logger.info("║ 调用类型: {}", callType);
        logger.info("║ 用户消息: {}", userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage);
        if (systemPrompt != null) {
            logger.info("║ 系统提示: {}", systemPrompt.length() > 100 ? systemPrompt.substring(0, 100) + "..." : systemPrompt);
        }
        logger.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    private void logDeepSeekResponse(String response, long duration) {
        logger.info("╔══════════════════════════════════════════════════════════════╗");
        logger.info("║           DeepSeek API 响应");
        logger.info("╠══════════════════════════════════════════════════════════════╣");
        logger.info("║ 响应时间: {} ms", duration);
        logger.info("║ 响应内容: {}", response.length() > 200 ? response.substring(0, 200) + "..." : response);
        logger.info("╚══════════════════════════════════════════════════════════════╝");
    }
}