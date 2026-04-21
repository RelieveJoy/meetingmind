package com.meetingmind.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 摘要消息推送服务
 * 
 * 负责处理摘要相关的 WebSocket 消息推送
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSummaryService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * 发送摘要开始消息
     */
    public void sendSummaryStart(String sessionId, String triggerReason, int pendingTextCount) {
        String summaryId = "sum_" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> message = Map.of(
            "type", "SUMMARY_START",
            "sessionId", sessionId,
            "data", Map.of(
                "summaryId", summaryId,
                "triggerReason", triggerReason,
                "pendingTextCount", pendingTextCount
            ),
            "timestamp", LocalDateTime.now().format(FORMATTER)
        );
        
        sendMessage(sessionId, message);
    }
    
    /**
     * 发送摘要更新消息
     */
    public void sendSummaryUpdate(String sessionId, String summaryId, String summary, java.util.List<String> keyPoints, boolean isIncremental, long durationMs) {
        Map<String, Object> message = Map.of(
            "type", "SUMMARY_UPDATE",
            "sessionId", sessionId,
            "data", Map.of(
                "summaryId", summaryId,
                "summary", summary,
                "keyPoints", keyPoints,
                "isIncremental", isIncremental,
                "durationMs", durationMs
            ),
            "timestamp", LocalDateTime.now().format(FORMATTER)
        );
        
        sendMessage(sessionId, message);
    }
    
    /**
     * 发送摘要错误消息
     */
    public void sendSummaryError(String sessionId, String summaryId, String errorCode, String errorMessage) {
        Map<String, Object> message = Map.of(
            "type", "SUMMARY_ERROR",
            "sessionId", sessionId,
            "data", Map.of(
                "summaryId", summaryId,
                "errorCode", errorCode,
                "errorMessage", errorMessage
            ),
            "timestamp", LocalDateTime.now().format(FORMATTER)
        );
        
        sendMessage(sessionId, message);
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(String sessionId, Map<String, Object> message) {
        try {
            // 尝试发送消息，最多重试3次
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    messagingTemplate.convertAndSend("/topic/summary/" + sessionId, message);
                    log.debug("WebSocket消息发送成功: sessionId={}, type={}", sessionId, message.get("type"));
                    return;
                } catch (Exception e) {
                    log.warn("WebSocket消息发送失败 ({}): sessionId={}, error={}", i + 1, sessionId, e.getMessage());
                    if (i == maxRetries - 1) {
                        log.error("WebSocket消息发送最终失败: sessionId={}", sessionId, e);
                    } else {
                        // 短暂延迟后重试
                        Thread.sleep(100 * (i + 1));
                    }
                }
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息时发生异常: sessionId={}", sessionId, e);
        }
    }
}

