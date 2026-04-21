package com.feishu.miji.service.segmentation;

import com.feishu.miji.entity.TextBuffer;
import com.feishu.miji.entity.TranscriptionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文本缓冲服务
 * 
 * 功能说明：
 * - 管理每个会话的文本缓冲
 * - 支持动态配置最大段数和等待时间
 * - 提供缓冲状态查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextBufferService implements com.feishu.miji.service.TextBufferService {
    
    /**
     * 默认最大缓冲段数
     */
    @Value("${buffer.max-segments:3}")
    private int defaultMaxSegments;
    
    /**
     * 默认最大等待秒数
     */
    @Value("${buffer.max-wait-seconds:60}")
    private int defaultMaxWaitSeconds;
    
    /**
     * 会话文本缓冲映射
     */
    private final Map<String, TextBuffer> sessionBuffers = new ConcurrentHashMap<>();
    
    /**
     * 添加转写文本到缓冲区
     * 
     * @param sessionId 会话ID
     * @param transcription 转写结果
     * @return 是否满足刷新条件
     */
    public boolean addText(String sessionId, TranscriptionResult transcription) {
        TextBuffer buffer = getOrCreateBuffer(sessionId);
        boolean shouldFlush = buffer.addText(transcription);
        
        log.debug("会话 {} 添加文本到缓冲，当前段数 {}/{}", 
                sessionId, buffer.getSegmentCount(), buffer.getMaxSegments());
        
        return shouldFlush;
    }
    
    /**
     * 获取并清除满足条件的缓冲内容
     * 
     * @param sessionId 会话ID
     * @return 待优化的文本列表（可能为空）
     */
    public List<TranscriptionResult> flushBuffer(String sessionId) {
        TextBuffer buffer = sessionBuffers.get(sessionId);
        if (buffer == null || buffer.isProcessing()) {
            return new ArrayList<>();
        }
        
        synchronized (buffer) {
            if (!buffer.shouldFlush()) {
                return new ArrayList<>();
            }
            
            buffer.setProcessing(true);
            try {
                List<TranscriptionResult> result = buffer.flush();
                log.info("会话 {} 刷新缓冲，提取 {} 段文本", sessionId, result.size());
                return result;
            } finally {
                buffer.setProcessing(false);
            }
        }
    }
    
    /**
     * 强制刷新缓冲（会话结束时使用）
     * 
     * @param sessionId 会话ID
     * @return 待优化的文本列表
     */
    public List<TranscriptionResult> forceFlushBuffer(String sessionId) {
        TextBuffer buffer = sessionBuffers.remove(sessionId);
        if (buffer == null) {
            return new ArrayList<>();
        }
        
        List<TranscriptionResult> result = buffer.flush();
        if (!result.isEmpty()) {
            log.info("会话 {} 强制刷新剩余 {} 段文本", sessionId, result.size());
        }
        return result;
    }
    
    /**
     * 获取缓冲区状态
     * 
     * @param sessionId 会话ID
     * @return 缓冲状态信息
     */
    public TextBuffer.BufferStatus getBufferStatus(String sessionId) {
        TextBuffer buffer = sessionBuffers.get(sessionId);
        if (buffer == null) {
            return TextBuffer.BufferStatus.builder()
                    .sessionId(sessionId)
                    .pendingCount(0)
                    .maxSegments(defaultMaxSegments)
                    .waitingSeconds(0)
                    .processing(false)
                    .build();
        }
        return buffer.getStatus();
    }
    
    /**
     * 检查缓冲是否满足优化条件
     * 
     * @param sessionId 会话ID
     * @return 是否应该触发优化
     */
    public boolean shouldOptimize(String sessionId) {
        TextBuffer buffer = sessionBuffers.get(sessionId);
        return buffer != null && buffer.shouldFlush();
    }
    
    /**
     * 获取或创建缓冲
     */
    private TextBuffer getOrCreateBuffer(String sessionId) {
        return sessionBuffers.computeIfAbsent(sessionId, id -> 
            TextBuffer.builder()
                    .sessionId(id)
                    .pendingTexts(new ArrayList<>())
                    .maxSegments(defaultMaxSegments)
                    .maxWaitSeconds(defaultMaxWaitSeconds)
                    .lastFlushTime(System.currentTimeMillis())
                    .segmentCount(0)
                    .processing(false)
                    .build()
        );
    }
    
    /**
     * 清理指定会话的缓冲
     */
    public void clearBuffer(String sessionId) {
        TextBuffer buffer = sessionBuffers.remove(sessionId);
        if (buffer != null) {
            log.debug("会话 {} 的文本缓冲已清理", sessionId);
        }
    }
    
    /**
     * 清理所有缓冲
     */
    public void clearAllBuffers() {
        sessionBuffers.clear();
        log.info("所有会话的文本缓冲已清理");
    }
    
    /**
     * 获取活跃缓冲数量
     */
    public int getActiveBufferCount() {
        return sessionBuffers.size();
    }
}
