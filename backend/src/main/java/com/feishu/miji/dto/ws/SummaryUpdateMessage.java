package com.feishu.miji.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebSocket总结更新消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryUpdateMessage {
    
    /** 消息类型: SUMMARY_UPDATE */
    private String type;
    
    /** 会话ID */
    private String sessionId;
    
    /** 总结数据 */
    private SummaryData data;
    
    /** 时间戳 */
    private String timestamp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryData {
        /** 总结ID */
        private String summaryId;
        
        /** 总结内容 */
        private String summary;
        
        /** 关键点列表 */
        private List<String> keyPoints;
    }
}
