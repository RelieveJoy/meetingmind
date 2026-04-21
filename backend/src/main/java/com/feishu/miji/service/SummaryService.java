package com.feishu.miji.service;

import com.feishu.miji.config.OllamaProperties;
import com.feishu.miji.config.SummaryProperties;
import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.Summary;
import com.feishu.miji.entity.SummaryRecord;
import com.feishu.miji.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 摘要生成服务
 * 
 * 功能说明：
 * - 调用 Ollama 大模型生成会议/录音摘要
 * - 提取关键点和行动项
 * - 支持增量总结
 * - 实现滑动窗口 + 增量更新策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {
    
    private final OllamaProperties ollamaProperties;
    private final SummaryProperties summaryProperties;
    private final SummaryHistoryService summaryHistoryService;
    private final WebSocketSummaryService webSocketSummaryService;
    private final ObjectMapper objectMapper;

    private OkHttpClient httpClient;

    /**
     * 初始化 HTTP 客户端
     */
    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(summaryProperties.getModel().getTimeout()))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 生成摘要
     * 
     * @param sessionId 会话ID
     * @param texts 已优化的文本列表
     * @param triggerReason 触发原因
     * @return 摘要结果
     */
    public Summary generateSummary(String sessionId, List<OptimizationResult> texts, String triggerReason) {
        log.info("开始生成摘要，会话ID: {}, 文本数量: {}, 触发原因: {}", sessionId, texts.size(), triggerReason);
        
        String summaryId = "sum_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        
        try {
            // 发送摘要开始消息
            sendSummaryStart(sessionId, triggerReason, texts.size());
            
            // 合并所有文本
            String combinedText = texts.stream()
                    .map(OptimizationResult::getOptimizedText)
                    .reduce("", (a, b) -> a + "\n" + b);
            
            // 构建摘要提示词
            String prompt = buildFirstSummaryPrompt(combinedText);
            
            // 调用 Ollama API
            Map<String, Object> requestBody = Map.of(
                    "model", summaryProperties.getModel().getName(),
                    "prompt", prompt,
                    "stream", false,
                    "temperature", summaryProperties.getModel().getTemperature()
            );
            
            Request request = new Request.Builder()
                    .url(ollamaProperties.getBaseUrl() + "/api/generate")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
            
            String summaryContent;
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("摘要生成请求失败: " + response);
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                summaryContent = (String) result.get("response");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            // 解析摘要内容
            Summary summary = parseSummaryResponse(sessionId, summaryContent, combinedText, durationMs);
            
            // 保存摘要记录
            saveSummaryRecord(sessionId, summary, triggerReason, durationMs, combinedText);
            
            // 发送 WebSocket 消息
            sendWebSocketMessage(sessionId, summaryId, summary.getContent(), summary.getKeyPoints(), false, durationMs);
            
            log.info("摘要生成完成，会话ID: {}, 耗时: {}ms", sessionId, durationMs);
            return summary;
            
        } catch (Exception e) {
            log.error("摘要生成失败，会话ID: {}", sessionId, e);
            sendSummaryError(sessionId, summaryId, "摘要生成失败: " + e.getMessage());
            throw new RuntimeException("摘要生成失败", e);
        }
    }
    
    /**
     * 从文本生成摘要
     */
    public Summary generateSummaryFromText(String sessionId, String text) {
        log.info("从文本生成摘要，会话ID: {}", sessionId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            String prompt = buildFirstSummaryPrompt(text);
            
            Map<String, Object> requestBody = Map.of(
                    "model", summaryProperties.getModel().getName(),
                    "prompt", prompt,
                    "stream", false,
                    "temperature", summaryProperties.getModel().getTemperature()
            );
            
            Request request = new Request.Builder()
                    .url(ollamaProperties.getBaseUrl() + "/api/generate")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
            
            String summaryContent;
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("摘要生成请求失败: " + response);
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                summaryContent = (String) result.get("response");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            Summary summary = parseSummaryResponse(sessionId, summaryContent, text, durationMs);
            
            log.info("摘要生成完成，会话ID: {}, 耗时: {}ms", sessionId, durationMs);
            return summary;
            
        } catch (Exception e) {
            log.error("摘要生成失败，会话ID: {}", sessionId, e);
            throw new RuntimeException("摘要生成失败", e);
        }
    }
    
    /**
     * 生成增量摘要（实时更新）
     * 
     * @param sessionId 会话ID
     * @param currentSummary 当前摘要
     * @param newText 新增文本
     * @param triggerReason 触发原因
     * @return 更新后的摘要
     */
    public Summary generateIncrementalSummary(String sessionId, Summary currentSummary, String newText, String triggerReason) {
        log.info("生成增量摘要，会话ID: {}", sessionId);
        
        String summaryId = "sum_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        
        try {
            // 发送摘要开始消息
            sendSummaryStart(sessionId, triggerReason, 1);
            
            String prompt = buildIncrementalSummaryPrompt(currentSummary.getContent(), newText);
            
            Map<String, Object> requestBody = Map.of(
                    "model", summaryProperties.getModel().getName(),
                    "prompt", prompt,
                    "stream", false,
                    "temperature", summaryProperties.getModel().getTemperature()
            );
            
            Request request = new Request.Builder()
                    .url(ollamaProperties.getBaseUrl() + "/api/generate")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")
                    ))
                    .build();
            
            String updatedContent;
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("增量摘要请求失败: " + response);
                }
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                updatedContent = (String) result.get("response");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            // 更新摘要
            currentSummary.setContent(updatedContent.trim());
            currentSummary.setGeneratedAt(LocalDateTime.now());
            currentSummary.setDurationMs(durationMs);
            
            // 保存摘要记录
            saveSummaryRecord(sessionId, currentSummary, triggerReason, durationMs, newText);
            
            // 发送 WebSocket 消息
            sendWebSocketMessage(sessionId, summaryId, currentSummary.getContent(), currentSummary.getKeyPoints(), true, durationMs);
            
            return currentSummary;
            
        } catch (Exception e) {
            log.error("增量摘要生成失败，会话ID: {}", sessionId, e);
            sendSummaryError(sessionId, summaryId, "增量摘要生成失败: " + e.getMessage());
            throw new RuntimeException("增量摘要生成失败", e);
        }
    }
    
    /**
     * 构建首次摘要提示词
     */
    private String buildFirstSummaryPrompt(String text) {
        return String.format("""
                ## 任务
                你是一个会议记录助手，负责生成会议摘要。
                
                ## 输入
                【会议内容】
                %s
                
                ## 要求
                1. 准确概括会议/录音的主要内容
                2. 提取关键信息和要点
                3. 识别主要话题和讨论方向
                
                ## 输出格式
                请按以下JSON格式输出：
                {
                  "summary": "会议摘要（200字以内）",
                  "keyPoints": ["关键点1", "关键点2", ...],
                  "topics": ["话题1", "话题2", ...]
                }
                """, text);
    }
    
    /**
     * 构建增量摘要提示词
     */
    private String buildIncrementalSummaryPrompt(String previousSummary, String newContent) {
        return String.format("""
                ## 任务
                你是一个会议记录助手，负责生成和更新会议摘要。
                
                ## 输入
                【历史摘要】
                %s
                
                【新增内容】
                %s
                
                ## 要求
                1. 结合历史摘要和新增内容，生成更新的摘要
                2. 保持摘要的连贯性和完整性
                3. 突出新增内容中的关键信息
                4. 如果发现与历史摘要矛盾的信息，进行修正
                
                ## 输出格式
                请按以下JSON格式输出：
                {
                  "summary": "更新后的完整摘要（200字以内）",
                  "keyPoints": ["关键点1", "关键点2", ...],
                  "newKeyPoints": ["本次新增的关键点"],
                  "topics": ["话题1", "话题2", ...]
                }
                """, previousSummary, newContent);
    }
    
    /**
     * 解析摘要响应
     */
    private Summary parseSummaryResponse(String sessionId, String content, String sourceText, long durationMs) {
        try {
            // 尝试解析JSON格式
            Map<String, Object> result = objectMapper.readValue(content, Map.class);
            
            String summaryText = (String) result.get("summary");
            List<String> keyPoints = (List<String>) result.getOrDefault("keyPoints", new ArrayList<>());
            List<String> topics = (List<String>) result.getOrDefault("topics", new ArrayList<>());
            
            return Summary.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .title("会议摘要")
                    .content(summaryText != null ? summaryText.trim() : content.trim())
                    .keyPoints(keyPoints)
                    .topics(topics)
                    .sourceText(sourceText)
                    .durationMs(durationMs)
                    .generatedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            // JSON解析失败，使用原始内容
            log.warn("摘要响应解析失败，使用原始内容: {}", e.getMessage());
            return Summary.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .title("会议摘要")
                    .content(content.trim())
                    .keyPoints(new ArrayList<>())
                    .topics(new ArrayList<>())
                    .sourceText(sourceText)
                    .durationMs(durationMs)
                    .generatedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }
    
    /**
     * 保存摘要记录
     */
    private void saveSummaryRecord(String sessionId, Summary summary, String triggerReason, long durationMs, String sourceText) {
        try {
            // 获取历史记录数量，计算版本号
            int version = summaryHistoryService.getHistory(sessionId).size() + 1;
            
            SummaryRecord record = SummaryRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .version(version)
                    .content(summary.getContent())
                    .keyPoints(summary.getKeyPoints())
                    .triggerReason(triggerReason)
                    .durationMs(durationMs)
                    .createdAt(LocalDateTime.now())
                    .sourceTextHash(calculateTextHash(sourceText))
                    .build();
            
            summaryHistoryService.saveRecord(record);
        } catch (Exception e) {
            log.error("保存摘要记录失败", e);
        }
    }
    
    /**
     * 计算文本哈希值
     */
    private String calculateTextHash(String text) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("计算文本哈希失败", e);
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * 发送 WebSocket 消息
     */
    private void sendWebSocketMessage(String sessionId, String summaryId, String summary, java.util.List<String> keyPoints, boolean isIncremental, long durationMs) {
        webSocketSummaryService.sendSummaryUpdate(sessionId, summaryId, summary, keyPoints, isIncremental, durationMs);
    }
    
    /**
     * 发送摘要错误消息
     */
    private void sendSummaryError(String sessionId, String summaryId, String errorMessage) {
        webSocketSummaryService.sendSummaryError(sessionId, summaryId, "MODEL_ERROR", errorMessage);
    }
    
    /**
     * 发送摘要开始消息
     */
    private void sendSummaryStart(String sessionId, String triggerReason, int pendingTextCount) {
        webSocketSummaryService.sendSummaryStart(sessionId, triggerReason, pendingTextCount);
    }
}
