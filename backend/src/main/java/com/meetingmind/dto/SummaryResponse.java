package com.meetingmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 总结响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {
    
    /**
     * 总结ID
     */
    private String summaryId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 总结标题
     */
    private String title;
    
    /**
     * 总结内容
     */
    private String content;
    
    /**
     * 关键要点
     */
    private List<String> keyPoints;
    
    /**
     * 行动项
     */
    private List<ActionItem> actionItems;
    
    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 内部类：行动项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String content;
        private String assignee;
        private String deadline;
    }
}


