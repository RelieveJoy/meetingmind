package com.feishu.miji.service;

public interface SegmentSchedulerService {
    /**
     * 启动会话的分段定时器
     * @param sessionId 会话ID
     */
    void startScheduler(String sessionId);

    /**
     * 停止会话的分段定时器
     * @param sessionId 会话ID
     */
    void stopScheduler(String sessionId);

    /**
     * 检查定时器状态
     * @param sessionId 会话ID
     * @return 是否运行中
     */
    boolean isRunning(String sessionId);
}