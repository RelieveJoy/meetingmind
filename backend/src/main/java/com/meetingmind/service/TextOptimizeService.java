package com.meetingmind.service;

import com.meetingmind.dto.*;

/**
 * 文本优化服务接口
 */
public interface TextOptimizeService {
    
    /**
     * 优化文本
     */
    OptimizeTextResponse optimizeText(OptimizeTextRequest request);
    
    /**
     * 生成总结
     */
    SummaryResponse generateSummary(GenerateSummaryRequest request);
    
    /**
     * 检查服务健康状态
     */
    boolean isServiceHealthy();
}


