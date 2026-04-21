package com.feishu.miji.service.segmentation;

import com.feishu.miji.entity.AudioSegment;
import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.SessionContext;
import com.feishu.miji.entity.Summary;
import com.feishu.miji.entity.TranscriptionResult;
import com.feishu.miji.service.AudioTranscribeService;
import com.feishu.miji.service.ModelProvider;
import com.feishu.miji.service.OptimizationCallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 分段优化服务 - 核心编排服务
 * 
 * 功能说明：
 * - 协调分段调度、文本缓冲、模型调用、回调通知
 * - 处理音频分段 → 转写 → 缓冲 → 优化 → 回调 的完整流程
 * - 管理会话生命周期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentOptimizationService {
    
    private final SegmentSchedulerService schedulerService;
    private final TextBufferService bufferService;
    private final ModelProvider modelProvider;
    private final CallbackRegistry callbackRegistry;
    private final SessionManagementService sessionService;
    private final AudioTranscribeService audioTranscribeService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 初始化服务
     */
    public void init() {
        // 设置分段处理器
        schedulerService.setSegmentProcessor(this::processAudioSegment);
        log.info("分段优化服务初始化完成");
    }
    
    /**
     * 开始录音会话
     * 
     * @param sessionId 会话ID（可为null，自动创建）
     * @return 会话ID
     */
    public String startSession(String sessionId) {
        // 创建或验证会话
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = sessionService.createSession();
        } else {
            sessionService.getOrCreateSession(sessionId);
        }
        
        // 更新会话状态
        sessionService.updateSessionState(sessionId, 
                SessionContext.SessionState.RECORDING);
        
        // 启动分段定时器
        schedulerService.startScheduler(sessionId);
        
        log.info("会话 {} 开始录音", sessionId);
        return sessionId;
    }
    
    /**
     * 结束录音会话
     * 
     * @param sessionId 会话ID
     */
    public void endSession(String sessionId) {
        log.info("会话 {} 结束录音", sessionId);
        
        // 停止分段定时器（会处理剩余音频）
        schedulerService.stopScheduler(sessionId);
        
        // 刷新剩余文本缓冲
        List<TranscriptionResult> remaining = bufferService.forceFlushBuffer(sessionId);
        if (!remaining.isEmpty()) {
            executeOptimization(sessionId, remaining);
        }
        
        // 生成最终摘要
        generateFinalSummary(sessionId);
        
        // 更新会话状态
        sessionService.updateSessionState(sessionId,
                com.feishu.miji.entity.SessionContext.SessionState.ENDED);
        
        // 触发会话结束回调
        Summary finalSummary = sessionService.getSession(sessionId).getCurrentSummary();
        callbackRegistry.triggerSessionEnd(sessionId, finalSummary);
        
        // 清理回调
        callbackRegistry.unregister(sessionId);
        
        log.info("会话 {} 已结束，处理了 {} 段文本", 
                sessionId, sessionService.getSession(sessionId).getTotalSegmentsProcessed());
    }
    
    /**
     * 处理音频分段
     * 
     * @param segment 音频分段
     */
    public void processAudioSegment(AudioSegment segment) {
        String sessionId = segment.getSessionId();
        log.debug("处理音频分段 #{}，时长 {:.1f}s", 
                segment.getIndex(), segment.getEndTime() - segment.getStartTime());
        
        // 更新会话状态
        sessionService.updateSessionState(sessionId, 
                com.feishu.miji.entity.SessionContext.SessionState.PROCESSING);
        
        try {
            // 调用转写服务
            CompletableFuture<TranscriptionResult> future = 
                    audioTranscribeService.transcribeAsync(segment);
            
            future.thenAccept(result -> {
                if (result != null) {
                    processTranscription(result);
                }
            }).exceptionally(e -> {
                log.error("分段 {} 转写失败", segment.getSegmentId(), e);
                callbackRegistry.triggerOptimizationError(sessionId, 
                        "转写失败: " + e.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            log.error("处理音频分段异常", e);
            callbackRegistry.triggerOptimizationError(sessionId, e.getMessage());
        }
    }
    
    /**
     * 处理转写结果
     * 
     * @param transcription 转写结果
     */
    public void processTranscription(TranscriptionResult transcription) {
        String sessionId = transcription.getSessionId();
        
        // 添加到文本缓冲
        boolean shouldFlush = bufferService.addText(sessionId, transcription);
        
        if (shouldFlush) {
            // 满足条件，触发优化
            List<TranscriptionResult> pending = bufferService.flushBuffer(sessionId);
            if (!pending.isEmpty()) {
                executeOptimization(sessionId, pending);
            }
        }
        
        // 更新会话统计
        var context = sessionService.getSession(sessionId);
        if (context != null) {
            context.setTotalSegmentsProcessed(context.getTotalSegmentsProcessed() + 1);
            context.setTotalTranscribedSeconds(
                    context.getTotalTranscribedSeconds() + 
                    (transcription.getEndTime() - transcription.getStartTime()));
        }
    }
    
    /**
     * 执行优化
     * 
     * @param sessionId 会话ID
     * @param texts 待优化文本列表
     * @return 优化结果
     */
    public OptimizationResult executeOptimization(String sessionId, 
            List<TranscriptionResult> texts) {
        log.info("会话 {} 开始优化 {} 段文本", sessionId, texts.size());
        
        try {
            // 获取会话上下文作为优化上下文
            var context = sessionService.getSession(sessionId);
            String optimizationContext = buildOptimizationContext(context);
            
            // 调用模型优化
            OptimizationResult result = modelProvider.optimize(texts, optimizationContext);
            
            if (result != null) {
                // 添加到会话记录
                sessionService.addOptimizationResult(sessionId, result);
                
                // 触发优化完成回调
                callbackRegistry.triggerOptimizationComplete(sessionId, result);
                
                // 发送 WebSocket 消息
                sendOptimizationResult(sessionId, result);
                
                // 更新摘要
                updateSummary(sessionId, result.getOptimizedText());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("会话 {} 优化失败", sessionId, e);
            callbackRegistry.triggerOptimizationError(sessionId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 更新摘要
     */
    private void updateSummary(String sessionId, String newOptimizedText) {
        var context = sessionService.getSession(sessionId);
        if (context == null) {
            return;
        }
        
        Summary currentSummary = context.getCurrentSummary();
        String textToSummarize;
        
        if (currentSummary == null || currentSummary.getContent().isBlank()) {
            textToSummarize = newOptimizedText;
        } else {
            // 增量更新摘要
            textToSummarize = currentSummary.getContent() + "\n" + newOptimizedText;
        }
        
        try {
            Summary newSummary = modelProvider.generateSummary(textToSummarize);
            sessionService.updateSummary(sessionId, newSummary);
            callbackRegistry.triggerSummaryUpdate(sessionId, newSummary);
            
        } catch (Exception e) {
            log.error("更新摘要失败", e);
        }
    }
    
    /**
     * 生成最终摘要
     */
    private void generateFinalSummary(String sessionId) {
        var context = sessionService.getSession(sessionId);
        if (context == null) {
            return;
        }
        
        // 合并所有优化结果
        StringBuilder fullText = new StringBuilder();
        for (OptimizationResult result : context.getCompletedOptimizations()) {
            if (result.getOptimizedText() != null) {
                fullText.append(result.getOptimizedText()).append("\n");
            }
        }
        
        if (fullText.length() > 0) {
            try {
                Summary finalSummary = modelProvider.generateSummary(fullText.toString());
                sessionService.updateSummary(sessionId, finalSummary);
                callbackRegistry.triggerSummaryUpdate(sessionId, finalSummary);
                
                log.info("会话 {} 生成最终摘要: {} 字", 
                        sessionId, finalSummary.getContent().length());
                
            } catch (Exception e) {
                log.error("生成最终摘要失败", e);
            }
        }
    }
    
    /**
     * 发送 WebSocket 优化结果
     */
    private void sendOptimizationResult(String sessionId, OptimizationResult result) {
        try {
            var message = java.util.Map.of(
                    "type", "OPTIMIZATION_RESULT",
                    "sessionId", sessionId,
                    "data", java.util.Map.of(
                            "id", result.getId(),
                            "originalText", result.getOriginalText(),
                            "optimizedText", result.getOptimizedText(),
                            "optimizationType", result.getOptimizationType(),
                            "durationMs", result.getDurationMs(),
                            "timestamp", System.currentTimeMillis()
                    )
            );
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
            
        } catch (Exception e) {
            log.warn("发送 WebSocket 消息失败", e);
        }
    }
    
    /**
     * 构建优化上下文
     */
    private String buildOptimizationContext(
            com.feishu.miji.entity.SessionContext context) {
        if (context == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("会话时长: ").append(context.getTotalTranscribedSeconds()).append("秒\n");
        sb.append("已处理段数: ").append(context.getTotalSegmentsProcessed()).append("\n");
        
        Summary currentSummary = context.getCurrentSummary();
        if (currentSummary != null && !currentSummary.getContent().isBlank()) {
            sb.append("当前摘要: ").append(currentSummary.getContent()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 注册优化回调
     */
    public void registerCallback(String sessionId, OptimizationCallback callback) {
        callbackRegistry.register(sessionId, callback);
    }
    
    /**
     * 获取会话状态
     */
    public com.feishu.miji.entity.SessionContext getSessionStatus(String sessionId) {
        return sessionService.getSession(sessionId);
    }
    
    /**
     * 获取模型健康状态
     */
    public boolean isModelHealthy() {
        return modelProvider.isHealthy();
    }
}
