package com.feishu.miji.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转写请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscribeRequest {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 音频数据（Base64编码）
     */
    private String audioData;
    
    /**
     * 语言
     */
    private String language = "zh";
    
    /**
     * 开始时间（秒）
     */
    private Double startTime;
    
    /**
     * 结束时间（秒）
     */
    private Double endTime;
    
    /**
     * 音频格式（如：wav, mp3, pcm）
     */
    private String format = "wav";
    
    /**
     * 采样率
     */
    private Integer sampleRate;
    
    /**
     * 声道数（1-单声道，2-立体声）
     */
    private Integer channels;
    
    /**
     * 是否启用说话人分离
     */
    private Boolean enableSpeakerDiarization;
}
