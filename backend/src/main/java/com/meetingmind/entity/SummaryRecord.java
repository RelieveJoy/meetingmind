package com.meetingmind.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 摘要记录实体类
 * 
 * 存储单次摘要的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryRecord {
    
    /**
     * 总结记录ID
     */
    private String id;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 版本号（增量更新的版本）
     */
    private int version;
    
    /**
     * 摘要内容
     */
    private String content;
    
    /**
     * 关键点
     */
    private List<String> keyPoints;
    
    /**
     * 触发原因
     */
    private String triggerReason;
    
    /**
     * 生成耗时
     */
    private long durationMs;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 源文本哈希（用于去重）
     */
    private String sourceTextHash;
}

