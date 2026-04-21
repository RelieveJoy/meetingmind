package com.feishu.miji.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 音频分段实体类
 * 
 * 用于描述录音中的一个时间片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioSegment {
    
    /**
     * 分段唯一标识
     */
    private String segmentId;
    
    /**
     * 所属会话ID
     */
    private String sessionId;
    
    /**
     * 分段序号（从0开始）
     */
    private int index;
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒）
     */
    private Double endTime;
    
    /**
     * 音频数据（字节数组）
     */
    private byte[] audioData;
    
    /**
     * 音频格式（如：wav, mp3, pcm）
     */
    private String format;
    
    /**
     * 采样率
     */
    private int sampleRate;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 是否已转写完成
     */
    private boolean transcribed;
    
    /**
     * 是否已优化完成
     */
    private boolean optimized;
}
