package com.feishu.miji.service.impl;

import com.feishu.miji.config.OllamaProperties;
import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.TranscriptionResult;
import com.feishu.miji.entity.Summary;
import com.feishu.miji.service.ModelProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ollama 模型提供者实现
 * 
 * 功能说明：
 * - 调用本地 Ollama 服务进行文本优化
 * - 支持流式输出和批量处理
 * - 预留接口可扩展云端 API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaModelProvider implements ModelProvider {
    
    /**
     * 优化提示词模板
     */
    private static final String OPTIMIZATION_PROMPT_TEMPLATE = """
            请优化以下转写文本，修正错别字、添加标点、优化格式，同时保持口语化风格。
            
            转写文本：
            %s
            
            要求：
            1. 只返回优化后的文本，不要添加解释说明
            2. 保持原意，不要添加或删除内容
            3. 标点符号使用中文
            4. 如果原文有明显错误才修正，不要过度修改
            
            优化后：
            """;
    
    /**
     * 摘要提示词模板
     */
    private static final String SUMMARY_PROMPT_TEMPLATE = """
            请为以下录音内容生成简洁摘要：
            
            内容：
            %s
            
            要求：
            1. 提取关键信息和主要观点
            2. 保持 100-200 字的长度
            3. 使用简洁清晰的语言
            4. 只返回摘要内容，不要添加"以下是摘要"等前缀
            
            摘要：
            """;
    
    private final OllamaProperties ollamaProperties;
    private final ObjectMapper objectMapper;
    
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(120))
            .writeTimeout(Duration.ofSeconds(30))
            .build();
    
    @Override
    public OptimizationResult optimize(List<TranscriptionResult> texts, String context) {
        if (texts == null || texts.isEmpty()) {
            return null;
        }
        
        log.info("开始优化 {} 段文本", texts.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // 合并文本
            String combinedText = combineTexts(texts);
            
            // 构建提示词
            String prompt = buildOptimizationPrompt(combinedText, context);
            
            // 调用 Ollama API
            String optimizedText = callOllamaGenerate(prompt, ollamaProperties.getOptimizeModel());
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            return OptimizationResult.builder()
                    .id(UUID.randomUUID().toString())
                    .transcriptionId(texts.get(0).getId())
                    .sessionId(texts.get(0).getSessionId())
                    .originalText(combinedText)
                    .optimizedText(optimizedText.trim())
                    .optimizationType("ai_optimization")
                    .optimizedAt(LocalDateTime.now())
                    .durationMs(durationMs)
                    .build();
            
        } catch (Exception e) {
            log.error("文本优化失败", e);
            return createErrorResult(texts, e);
        }
    }
    
    @Override
    public Summary generateSummary(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return Summary.builder()
                    .id(UUID.randomUUID().toString())
                    .content("")
                    .keywords(List.of())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        
        log.info("生成摘要，文本长度 {} 字", fullText.length());
        long startTime = System.currentTimeMillis();
        
        try {
            // 限制文本长度（防止超出模型上下文）
            String truncatedText = truncateText(fullText, 8000);
            
            // 构建提示词
            String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, truncatedText);
            
            // 调用 Ollama API
            String summaryContent = callOllamaGenerate(prompt, ollamaProperties.getSummaryModel());
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            return Summary.builder()
                    .id(UUID.randomUUID().toString())
                    .content(summaryContent.trim())
                    .keywords(extractKeywords(summaryContent))
                    .createdAt(LocalDateTime.now())
                    .durationMs(durationMs)
                    .build();
            
        } catch (Exception e) {
            log.error("摘要生成失败", e);
            return Summary.builder()
                    .id(UUID.randomUUID().toString())
                    .content("[摘要生成失败]")
                    .keywords(List.of())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }
    
    @Override
    public String getModelName() {
        return ollamaProperties.getSummaryModel(); // 默认返回总结模型
    }
    
    @Override
    public String getProviderName() {
        return "ollama";
    }
    
    @Override
    public boolean isHealthy() {
        try {
            Request request = new Request.Builder()
                    .url(ollamaProperties.getBaseUrl() + "/api/tags")
                    .get()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.warn("Ollama 健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 调用 Ollama generate API
     */
    private String callOllamaGenerate(String prompt, String model) throws IOException {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );
        
        Request request = new Request.Builder()
                .url(ollamaProperties.getBaseUrl() + "/api/generate")
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")
                ))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama 请求失败: " + response);
            }
            
            String responseBody = response.body() != null ? response.body().string() : "";
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
            return (String) result.get("response");
        }
    }
    
    /**
     * 合并多个转写文本
     */
    private String combineTexts(List<TranscriptionResult> texts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            TranscriptionResult t = texts.get(i);
            if (t.getSpeaker() != null) {
                sb.append("[").append(t.getSpeaker()).append("] ");
            }
            sb.append(t.getOriginalText());
            if (i < texts.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * 构建优化提示词
     */
    private String buildOptimizationPrompt(String text, String context) {
        String prompt = String.format(OPTIMIZATION_PROMPT_TEMPLATE, text);
        if (context != null && !context.isBlank()) {
            prompt = "背景信息：" + context + "\n\n" + prompt;
        }
        return prompt;
    }
    
    /**
     * 截断过长文本
     */
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * 提取关键词（简单实现）
     */
    private List<String> extractKeywords(String summary) {
        // TODO: 实现关键词提取
        return List.of();
    }
    
    /**
     * 创建错误结果
     */
    private OptimizationResult createErrorResult(List<TranscriptionResult> texts, Exception e) {
        String combinedText = combineTexts(texts);
        return OptimizationResult.builder()
                .id(UUID.randomUUID().toString())
                .transcriptionId(texts.get(0).getId())
                .sessionId(texts.get(0).getSessionId())
                .originalText(combinedText)
                .optimizedText("[优化失败: " + e.getMessage() + "]\n" + combinedText)
                .optimizationType("error_fallback")
                .optimizedAt(LocalDateTime.now())
                .durationMs(0)
                .build();
    }
}
