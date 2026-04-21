package com.feishu.miji.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 转写结果实体类
 * 
 * 存储 Whisper 模型转写后的文字结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResult {
    
    /**
     * 转写结果唯一标识
     */
    private String id;
    
    /**
     * 对应的分段ID
     */
    private String segmentId;
    
    /**
     * 所属会话ID
     */
    private String sessionId;
    
    /**
     * 原始转写文本（未经优化）
     */
    private String originalText;
    
    /**
     * 说话人标识（如：speaker_1, speaker_2）
     */
    private String speaker;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒）
     */
    private Double endTime;
    
    /**
     * 置信度（0-1）
     */
    private Double confidence;
    
    /**
     * 语言（如：zh, en）
     */
    private String language;
    
    /**
     * 转写完成时间
     */
    private LocalDateTime transcribedAt;
    
    /**
     * 转写耗时（毫秒）
     */
    private long durationMs;
}
