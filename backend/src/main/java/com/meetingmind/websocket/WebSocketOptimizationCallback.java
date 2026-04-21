package com.meetingmind.websocket;

import com.meetingmind.entity.OptimizationResult;
import com.meetingmind.entity.Summary;
import com.meetingmind.service.OptimizationCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 优化回调实现
 * 
 * 将优化结果通过 WebSocket 推送到前端
 */
@Slf4j
@Component
public class WebSocketOptimizationCallback implements OptimizationCallback {
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 回调名称
     */
    private static final String CALLBACK_NAME = "WebSocketCallback";
    
    @Override
    public String getCallbackName() {
        return CALLBACK_NAME;
    }
    
    @Override
    public void onOptimizationComplete(OptimizationResult result) {
        if (messagingTemplate == null || result == null) {
            return;
        }
        
        try {
            var message = Map.of(
                    "type", "OPTIMIZATION_RESULT",
                    "sessionId", result.getSessionId(),
                    "data", Map.of(
                            "id", result.getId(),
                            "originalText", result.getOriginalText(),
                            "optimizedText", result.getOptimizedText(),
                            "optimizationType", result.getOptimizationType(),
                            "durationMs", result.getDurationMs(),
                            "timestamp", System.currentTimeMillis()
                    )
            );
            
            messagingTemplate.convertAndSend(
                    "/topic/session/" + result.getSessionId(), 
                    message
            );
            
            log.debug("WebSocket 推送优化结果: {}", result.getId());
            
        } catch (Exception e) {
            log.error("推送优化结果失败", e);
        }
    }
    
    @Override
    public void onOptimizationError(String sessionId, String error) {
        if (messagingTemplate == null) {
            return;
        }
        
        try {
            var message = Map.of(
                    "type", "ERROR",
                    "sessionId", sessionId,
                    "data", Map.of(
                            "error", error,
                            "code", "OPTIMIZATION_ERROR",
                            "timestamp", System.currentTimeMillis()
                    )
            );
            
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId, 
                    message
            );
            
            log.warn("WebSocket 推送错误: session={}, error={}", sessionId, error);
            
        } catch (Exception e) {
            log.error("推送错误信息失败", e);
        }
    }
    
    @Override
    public void onSummaryUpdate(Summary summary) {
        if (messagingTemplate == null || summary == null) {
            return;
        }
        
        try {
            var message = Map.of(
                    "type", "SUMMARY_UPDATE",
                    "sessionId", summary.getSessionId() != null ? summary.getSessionId() : "",
                    "data", Map.of(
                            "id", summary.getId(),
                            "summary", summary.getContent(),
                            "keywords", summary.getKeywords() != null 
                                    ? summary.getKeywords() 
                                    : List.of(),
                            "timestamp", System.currentTimeMillis()
                    )
            );
            
            messagingTemplate.convertAndSend(
                    "/topic/session/" + summary.getSessionId() + "/summary", 
                    message
            );
            
            log.debug("WebSocket 推送摘要更新: {}", summary.getId());
            
        } catch (Exception e) {
            log.error("推送摘要更新失败", e);
        }
    }
    
    @Override
    public void onSessionEnd(String sessionId, Summary finalSummary) {
        if (messagingTemplate == null) {
            return;
        }
        
        try {
            var message = Map.of(
                    "type", "SESSION_ENDED",
                    "sessionId", sessionId,
                    "data", Map.of(
                            "summary", finalSummary != null 
                                    ? finalSummary.getContent() 
                                    : "",
                            "keywords", finalSummary != null && finalSummary.getKeywords() != null
                                    ? finalSummary.getKeywords()
                                    : List.of(),
                            "timestamp", System.currentTimeMillis()
                    )
            );
            
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId, 
                    message
            );
            
            log.info("WebSocket 推送会话结束: {}", sessionId);
            
        } catch (Exception e) {
            log.error("推送会话结束消息失败", e);
        }
    }
}


