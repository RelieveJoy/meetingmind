package com.meetingmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 转写响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscribeResponse {

    /**
     * 分段ID
     */
    private String segmentId;

    /**
     * 转写ID
     */
    private String transcriptionId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 转写文本
     */
    private String text;

    /**
     * 说话人标识
     */
    private String speaker;
    
    /**
     * 语言
     */
    private String language;

    /**
     * 开始时间（秒）
     */
    private Double startTime;

    /**
     * 结束时间（秒）
     */
    private Double endTime;

    /**
     * 置信度
     */
    private Double confidence;

    /**
     * 转写时间
     */
    private LocalDateTime transcribedAt;
}


