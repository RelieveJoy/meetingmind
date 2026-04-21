package com.feishu.miji.service;

import com.feishu.miji.entity.SessionContext;
import com.feishu.miji.service.segmentation.SessionManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 会话服务
 * 
 * 封装 SessionManagementService，提供简化的会话管理接口
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final SessionManagementService sessionManagementService;
    
    /**
     * 创建新会话
     */
    public String createSession() {
        return sessionManagementService.createSession();
    }
    
    /**
     * 获取会话上下文
     */
    public SessionContext getSession(String sessionId) {
        return sessionManagementService.getSession(sessionId);
    }
    
    /**
     * 结束会话
     */
    public void endSession(String sessionId) {
        sessionManagementService.destroySession(sessionId);
    }
    
    /**
     * 更新会话活动时间
     */
    public void updateActivity(String sessionId) {
        SessionContext context = sessionManagementService.getSession(sessionId);
        if (context != null) {
            context.setLastActivityAt(LocalDateTime.now());
        }
    }
    
    /**
     * 检查会话是否存在
     */
    public boolean exists(String sessionId) {
        return sessionManagementService.getSession(sessionId) != null;
    }
}
