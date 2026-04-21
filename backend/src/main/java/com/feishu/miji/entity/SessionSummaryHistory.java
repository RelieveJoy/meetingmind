package com.feishu.miji.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话摘要历史实体类
 * 
 * 管理单个会话的所有摘要记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryHistory {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 总结列表
     */
    private List<SummaryRecord> summaries;
    
    /**
     * 最新摘要
     */
    private String latestSummary;
    
    /**
     * 总结总数
     */
    private int totalSummaryCount;
    
    /**
     * 最后总结时间
     */
    private LocalDateTime lastSummaryTime;
}