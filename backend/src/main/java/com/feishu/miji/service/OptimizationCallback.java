package com.feishu.miji.service;

import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.Summary;

/**
 * 优化结果回调接口
 * 
 * 定义优化服务完成后的回调通知
 * 支持多个回调订阅同一个会话
 */
public interface OptimizationCallback {
    
    /**
     * 优化完成回调
     * 
     * @param result 优化结果
     */
    default void onOptimizationComplete(OptimizationResult result) {
        // 默认空实现
    }
    
    /**
     * 优化失败回调
     * 
     * @param sessionId 会话ID
     * @param error 错误信息
     */
    default void onOptimizationError(String sessionId, String error) {
        // 默认空实现
    }
    
    /**
     * 摘要更新回调
     * 
     * @param summary 摘要
     */
    default void onSummaryUpdate(Summary summary) {
        // 默认空实现
    }
    
    /**
     * 会话结束回调
     * 
     * @param sessionId 会话ID
     * @param finalSummary 最终摘要
     */
    default void onSessionEnd(String sessionId, Summary finalSummary) {
        // 默认空实现
    }
    
    /**
     * 获取回调名称（用于日志）
     */
    default String getCallbackName() {
        return this.getClass().getSimpleName();
    }
}
