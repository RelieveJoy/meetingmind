package com.meetingmind.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优化结果实体类
 * 
 * 存储 AI 优化后的文字结果
 * 优化包括：修正错别字、标点、格式调整等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResult {
    
    /**
     * 优化结果唯一标识
     */
    private String id;
    
    /**
     * 对应的转写结果ID
     */
    private String transcriptionId;
    
    /**
     * 所属会话ID
     */
    private String sessionId;
    
    /**
     * 原始文本（优化前）
     */
    private String originalText;
    
    /**
     * 优化后文本
     */
    private String optimizedText;
    
    /**
     * 优化类型（如：punctuation, spelling, format）
     */
    private String optimizationType;
    
    /**
     * 优化详情说明
     */
    private String details;
    
    /**
     * 优化完成时间
     */
    private LocalDateTime optimizedAt;
    
    /**
     * 优化耗时（毫秒）
     */
    private long durationMs;
}


