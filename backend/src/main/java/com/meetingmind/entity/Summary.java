package com.meetingmind.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 摘要实体类
 * 
 * 存储 AI 生成的会议/录音摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summary {
    
    /**
     * 摘要唯一标识
     */
    private String id;
    
    /**
     * 所属会话ID
     */
    private String sessionId;
    
    /**
     * 摘要标题
     */
    private String title;
    
    /**
     * 摘要内容（完整摘要文本）
     */
    private String content;
    
    /**
     * 关键点列表
     */
    private List<String> keyPoints;
    
    /**
     * 行动项列表（如有待办事项）
     */
    private List<String> actionItems;
    
    /**
     * 涉及的话题/主题
     */
    private List<String> topics;
    
    /**
     * 参与人数
     */
    private int participantCount;
    
    /**
     * 摘要生成时间
     */
    private LocalDateTime generatedAt;
    
    /**
     * 关联的转写文本（生成摘要所依据的原始文本）
     */
    private String sourceText;
    
    /**
     * 摘要类型（如：brief, detailed, bullet_points）
     */
    private String summaryType;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 摘要生成耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 摘要创建时间
     */
    private LocalDateTime createdAt;
}


