package com.meetingmind.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket转写结果消息
 * 
 * 服务器推送的实时转写结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptMessage {
    
    /** 消息类型: TRANSCRIPT */
    private String type;
    
    /** 会话ID */
    private String sessionId;
    
    /** 转写数据 */
    private TranscriptData data;
    
    /** 时间戳 */
    private String timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptData {
        /** 分段ID */
        private String segmentId;
        
        /** 转写文本 */
        private String text;
        
        /** 开始时间（秒） */
        private Double startTime;
        
        /** 结束时间（秒） */
        private Double endTime;
        
        /** 置信度 */
        private Double confidence;
        
        /** 是否为最终结果 */
        private Boolean isFinal;
    }
}


