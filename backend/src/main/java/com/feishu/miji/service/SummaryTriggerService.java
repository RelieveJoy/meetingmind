package com.feishu.miji.service;

/**
 * 摘要触发服务接口
 * 
 * 负责管理摘要生成的触发逻辑，包括时间触发、文本量触发等
 */
public interface SummaryTriggerService {
    
    /**
     * 检查是否需要触发总结
     * @param sessionId 会话ID
     * @return 是否需要触发
     */
    boolean shouldTriggerSummary(String sessionId);
    
    /**
     * 触发总结生成
     * @param sessionId 会话ID
     */
    void triggerSummary(String sessionId);
    
    /**
     * 注册文本更新回调
     * @param sessionId 会话ID
     * @param newText 新增文本
     */
    void onTextUpdated(String sessionId, String newText);
    
    /**
     * 重置触发状态（会话开始时调用）
     * @param sessionId 会话ID
     */
    void resetTriggerState(String sessionId);
    
    /**
     * 手动触发总结
     * @param sessionId 会话ID
     */
    void manualTrigger(String sessionId);
    
    /**
     * 会话结束时触发最终总结
     * @param sessionId 会话ID
     */
    void triggerFinalSummary(String sessionId);
}