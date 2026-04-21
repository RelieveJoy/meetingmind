package com.feishu.miji.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文本优化响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizeTextResponse {
    
    /**
     * 优化结果ID
     */
    private String optimizedId;
    
    /**
     * 分段ID
     */
    private String segmentId;
    
    /**
     * 原始文本
     */
    private String originalText;
    
    /**
     * 优化后的文本
     */
    private String optimizedText;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 优化时间
     */
    private LocalDateTime optimizedAt;
    
    /**
     * 处理耗时（毫秒）
     */
    private Long durationMs;
}
