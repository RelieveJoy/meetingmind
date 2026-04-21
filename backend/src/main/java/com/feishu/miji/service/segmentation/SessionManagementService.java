package com.feishu.miji.service.segmentation;

import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.SessionContext;
import com.feishu.miji.entity.Summary;
import com.feishu.miji.entity.TranscriptionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务
 * 
 * 功能说明：
 * - 管理所有活跃会话的生命周期
 * - 提供会话状态查询
 * - 维护会话上下文
 */
@Slf4j
@Service
public class SessionManagementService {
    
    /**
     * 会话上下文映射
     */
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    
    /**
     * 最大活跃会话数
     */
    private static final int MAX_ACTIVE_SESSIONS = 10;
    
    /**
     * 会话超时时间（分钟）
     */
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    
    /**
     * 创建新会话
     * 
     * @return 会话ID
     */
    public String createSession() {
        // 检查会话数量限制
        if (sessions.size() >= MAX_ACTIVE_SESSIONS) {
            log.warn("活跃会话数已达上限 {}，请先结束部分会话", MAX_ACTIVE_SESSIONS);
            return null;
        }
        
        String sessionId = java.util.UUID.randomUUID().toString();
        SessionContext context = SessionContext.builder()
                .sessionId(sessionId)
                .state(SessionContext.SessionState.CREATED)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .completedOptimizations(new java.util.concurrent.CopyOnWriteArrayList<>())
                .totalTranscribedSeconds(0.0)
                .totalSegmentsProcessed(0)
                .optimizationRequestCount(0)
                .optimizationSuccessCount(0)
                .build();
        
        sessions.put(sessionId, context);
        log.info("创建新会话: {}", sessionId);
        
        return sessionId;
    }
    
    /**
     * 获取会话上下文
     * 
     * @param sessionId 会话ID
     * @return 会话上下文（可能为null）
     */
    public SessionContext getSession(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.updateActivity();
        }
        return context;
    }
    
    /**
     * 获取或创建会话上下文
     */
    public SessionContext getOrCreateSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> {
            log.info("为会话 {} 创建新上下文", id);
            return SessionContext.builder()
                    .sessionId(id)
                    .state(SessionContext.SessionState.CREATED)
                    .createdAt(LocalDateTime.now())
                    .lastActivityAt(LocalDateTime.now())
                    .completedOptimizations(new java.util.concurrent.CopyOnWriteArrayList<>())
                    .build();
        });
    }
    
    /**
     * 销毁会话
     * 
     * @param sessionId 会话ID
     */
    public void destroySession(String sessionId) {
        SessionContext context = sessions.remove(sessionId);
        if (context != null) {
            context.setState(SessionContext.SessionState.ENDED);
            log.info("销毁会话: {}，处理了 {} 段，优化成功率 {:.1f}%", 
                    sessionId, 
                    context.getTotalSegmentsProcessed(),
                    context.getOptimizationSuccessRate() * 100);
        }
    }
    
    /**
     * 获取所有活跃会话
     */
    public List<String> getActiveSessions() {
        return sessions.values().stream()
                .filter(SessionContext::isValid)
                .map(SessionContext::getSessionId)
                .toList();
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(SessionContext::isValid)
                .count();
    }
    
    /**
     * 更新会话状态
     */
    public void updateSessionState(String sessionId, SessionContext.SessionState state) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.setState(state);
            context.updateActivity();
            log.debug("会话 {} 状态更新为 {}", sessionId, state);
        }
    }
    
    /**
     * 添加优化结果到会话
     */
    public void addOptimizationResult(String sessionId, OptimizationResult result) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.addOptimization(result);
            context.updateActivity();
        }
    }
    
    /**
     * 更新当前摘要
     */
    public void updateSummary(String sessionId, Summary summary) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.setCurrentSummary(summary);
            context.updateActivity();
        }
    }
    
    /**
     * 更新音频缓冲状态
     */
    public void updateAudioBufferStatus(String sessionId, 
            SessionContext.AudioBufferStatus audioStatus) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.setAudioBufferStatus(audioStatus);
            context.updateActivity();
        }
    }
    
    /**
     * 更新文本缓冲状态
     * src/main/java/com/feishu/miji/entity/TextBuffer.java
     */
    public void updateTextBufferStatus(String sessionId,
            com.feishu.miji.entity.TextBuffer.BufferStatus bufferStatus) {
        SessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.setTextBufferStatus(bufferStatus);
        }
    }
    
    /**
     * 清理超时会话
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupTimeoutSessions() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(SESSION_TIMEOUT_MINUTES);

        int cleanedCount = 0;
        for (Map.Entry<String, SessionContext> entry : sessions.entrySet()) {
            SessionContext context = entry.getValue();
            if (context.getLastActivityAt().isBefore(threshold)
                    && context.isValid()) {
                log.info("清理超时会话: {}", entry.getKey());
                sessions.remove(entry.getKey());
                cleanedCount++;
            }
        }
        if (cleanedCount > 0) {
            log.info("本次清理 {} 个超时会话，当前活跃会话数: {}", cleanedCount, getActiveSessionCount());
        }
    }
    
    /**
     * 获取会话详情
     */
    public Map<String, Object> getSessionDetails(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        if (context == null) {
            return Map.of("error", "会话不存在");
        }
        
        return Map.of(
                "sessionId", context.getSessionId(),
                "state", context.getState(),
                "createdAt", context.getCreatedAt(),
                "lastActivityAt", context.getLastActivityAt(),
                "totalSegments", context.getTotalSegmentsProcessed(),
                "totalTranscribedSeconds", context.getTotalTranscribedSeconds(),
                "optimizationSuccessRate", context.getOptimizationSuccessRate(),
                "currentSummary", context.getCurrentSummary() != null 
                        ? context.getCurrentSummary().getContent() 
                        : ""
        );
    }
}
