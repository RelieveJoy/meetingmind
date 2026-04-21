package com.feishu.miji.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feishu.miji.config.OllamaProperties;
import com.feishu.miji.dto.*;
import com.feishu.miji.service.TextOptimizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Ollama文本优化服务实现
 * 
 * 集成Ollama本地大模型进行文本优化和总结生成。
 */
@Service
public class OllamaTextOptimizeService implements TextOptimizeService {
    
    private static final Logger log = LoggerFactory.getLogger(OllamaTextOptimizeService.class);
    
    @Autowired
    private OllamaProperties ollamaProperties;

    @Lazy
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 文本优化提示词模板
     */
    private static final String OPTIMIZE_PROMPT_TEMPLATE = """
        请对以下文本进行润色优化，保持原意和语气，仅修正不通顺的表达，不要添加额外内容。
        
        原文：
        %s
        
        优化后的文本：
        """;
    
    /**
     * 总结生成提示词模板
     */
    private static final String SUMMARY_PROMPT_TEMPLATE = """
        请对以下会议/录音内容进行总结：
        
        内容：
        %s
        
        请按以下格式返回：
        1. 简要总结（100字以内）：
        2. 关键要点（3-5条）：
        """;
    
    @Override
    public OptimizeTextResponse optimizeText(OptimizeTextRequest request) {
        try {
            String prompt = String.format(OPTIMIZE_PROMPT_TEMPLATE, request.getText());
            
            // 调用Ollama API
            String response = callOllama(ollamaProperties.getOptimizeModel(), prompt);
            
            return OptimizeTextResponse.builder()
                    .optimizedId("opt_" + UUID.randomUUID().toString().substring(0, 8))
                    .segmentId(request.getSegmentId())
                    .originalText(request.getText())
                    .optimizedText(response.trim())
                    .model(ollamaProperties.getOptimizeModel())
                    .build();
                    
        } catch (Exception e) {
            log.error("文本优化失败: {}", e.getMessage());
            throw new RuntimeException("文本优化失败: " + e.getMessage());
        }
    }
    
    @Override
    public SummaryResponse generateSummary(GenerateSummaryRequest request) {
        try {
            String transcript = request.getText() != null ? request.getText() : "";

            String prompt = String.format(SUMMARY_PROMPT_TEMPLATE, transcript);

            String response = callOllama(ollamaProperties.getSummaryModel(), prompt);

            List<String> keyPoints = extractKeyPoints(response);
            String summary = extractSummary(response);

            return SummaryResponse.builder()
                    .summaryId("sum_" + UUID.randomUUID().toString().substring(0, 8))
                    .sessionId(request.getSessionId())
                    .content(summary)
                    .keyPoints(keyPoints)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("总结生成失败: {}", e.getMessage());
            throw new RuntimeException("总结生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 调用Ollama API
     */
    private String callOllama(String model, String prompt) {
        String ollamaUrl = ollamaProperties.getBaseUrl() + "/api/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of("temperature", 0.7));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(ollamaUrl, request, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("response").asText();
        } catch (Exception e) {
            return response;
        }
    }
    
    /**
     * 提取总结（简化处理）
     */
    private String extractSummary(String response) {
        // 实际生产环境需要更复杂的解析逻辑
        String[] lines = response.split("\n");
        StringBuilder summary = new StringBuilder();
        boolean foundSummary = false;
        
        for (String line : lines) {
            if (line.contains("总结") || foundSummary) {
                foundSummary = true;
                summary.append(line).append("\n");
                if (summary.length() > 200) break;
            }
        }
        
        return summary.length() > 0 ? summary.toString().trim() : response;
    }
    
    /**
     * 提取关键点（简化处理）
     */
    private List<String> extractKeyPoints(String response) {
        // 实际生产环境需要更复杂的解析逻辑
        List<String> points = new ArrayList<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            if (line.matches("^[0-9]+[.、].*") && line.length() > 10) {
                points.add(line.replaceFirst("^[0-9]+[.、]", "").trim());
            }
            if (points.size() >= 5) break;
        }
        
        return points;
    }
    
    /**
     * 检查服务健康状态
     */
    public boolean isServiceHealthy() {
        try {
            String healthUrl = ollamaProperties.getBaseUrl() + "/api/tags";
            HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());
            restTemplate.getForObject(healthUrl, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Ollama服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
