package com.meetingmind.entity;

import com.meetingmind.entity.TranscriptionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本缓冲实体类
 * 
 * 用于积累转写文本，满足条件后批量触发优化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextBuffer {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 当前缓冲的文本列表
     */
    private List<TranscriptionResult> pendingTexts;
    
    /**
     * 缓冲配置 - 最大段数
     */
    private int maxSegments;
    
    /**
     * 缓冲配置 - 最大等待秒数
     */
    private int maxWaitSeconds;
    
    /**
     * 上次刷新时间戳
     */
    private long lastFlushTime;
    
    /**
     * 当前积累段数
     */
    private int segmentCount;
    
    /**
     * 是否正在处理中（防止并发刷新）
     */
    private volatile boolean processing;
    
    /**
     * 添加文本到缓冲
     */
    public boolean addText(TranscriptionResult transcription) {
        if (pendingTexts == null) {
            pendingTexts = new ArrayList<>();
        }
        pendingTexts.add(transcription);
        segmentCount++;
        return shouldFlush();
    }
    
    /**
     * 判断是否应该刷新缓冲
     */
    public boolean shouldFlush() {
        // 段数达到上限
        if (segmentCount >= maxSegments) {
            return true;
        }
        // 等待超时
        if (lastFlushTime > 0) {
            long elapsed = (System.currentTimeMillis() - lastFlushTime) / 1000;
            if (elapsed >= maxWaitSeconds) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取并清除缓冲内容
     */
    public List<TranscriptionResult> flush() {
        List<TranscriptionResult> result = 
            pendingTexts != null ? new ArrayList<>(pendingTexts) 
                                  : new ArrayList<>();
        clear();
        lastFlushTime = System.currentTimeMillis();
        return result;
    }
    
    /**
     * 清除缓冲
     */
    public void clear() {
        if (pendingTexts != null) {
            pendingTexts.clear();
        }
        segmentCount = 0;
    }
    
    /**
     * 缓冲状态信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BufferStatus {
        private String sessionId;
        private int pendingCount;
        private int maxSegments;
        private long waitingSeconds;
        private boolean processing;
    }
    
    /**
     * 获取缓冲状态
     */
    public BufferStatus getStatus() {
        long waitingSeconds = 0;
        if (lastFlushTime > 0) {
            waitingSeconds = (System.currentTimeMillis() - lastFlushTime) / 1000;
        }
        return BufferStatus.builder()
                .sessionId(sessionId)
                .pendingCount(segmentCount)
                .maxSegments(maxSegments)
                .waitingSeconds(waitingSeconds)
                .processing(processing)
                .build();
    }
}


