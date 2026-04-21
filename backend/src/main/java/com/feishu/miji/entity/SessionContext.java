package com.feishu.miji.entity;

import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.Summary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话上下文
 * 
 * 存储会话的完整状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionContext {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话状态
     */
    private SessionState state;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后活动时间
     */
    private LocalDateTime lastActivityAt;
    
    /**
     * 音频缓冲状态
     */
    private AudioBufferStatus audioBufferStatus;
    
    /**
     * 文本缓冲状态
     */
    private TextBuffer.BufferStatus textBufferStatus;
    
    /**
     * 已完成的优化结果列表
     */
    private List<OptimizationResult> completedOptimizations;
    
    /**
     * 当前摘要
     */
    private Summary currentSummary;
    
    /**
     * 总转写时长（秒）
     */
    private Double totalTranscribedSeconds;
    
    /**
     * 总处理段数
     */
    private int totalSegmentsProcessed;
    
    /**
     * 优化请求次数
     */
    private int optimizationRequestCount;
    
    /**
     * 优化成功次数
     */
    private int optimizationSuccessCount;
    
    /**
     * 会话状态枚举
     */
    public enum SessionState {
        /**
         * 初始状态
         */
        CREATED,
        
        /**
         * 录音中
         */
        RECORDING,
        
        /**
         * 处理中
         */
        PROCESSING,
        
        /**
         * 已暂停
         */
        PAUSED,
        
        /**
         * 已结束
         */
        ENDED,
        
        /**
         * 异常状态
         */
        ERROR
    }
    
    /**
     * 音频缓冲状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioBufferStatus {
        /**
         * 当前缓冲字节数
         */
        private long bufferedBytes;
        
        /**
         * 缓冲音频时长（秒）
         */
        private Double bufferedSeconds;
        
        /**
         * 定时器是否运行
         */
        private boolean schedulerRunning;
        
        /**
         * 当前分段索引
         */
        private int currentSegmentIndex;
    }
    
    /**
     * 添加优化结果
     */
    public void addOptimization(OptimizationResult result) {
        if (completedOptimizations != null) {
            completedOptimizations.add(result);
            optimizationRequestCount++;
            if (result.getOptimizationType() != null && 
                !result.getOptimizationType().startsWith("error")) {
                optimizationSuccessCount++;
            }
        }
    }
    
    /**
     * 获取优化成功率
     */
    public double getOptimizationSuccessRate() {
        if (optimizationRequestCount == 0) {
            return 0.0;
        }
        return (double) optimizationSuccessCount / optimizationRequestCount;
    }
    
    /**
     * 更新活动时间
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * 是否有效会话
     */
    public boolean isValid() {
        return state != SessionState.ENDED && state != SessionState.ERROR;
    }
}
