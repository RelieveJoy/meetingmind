package com.feishu.miji.service.segmentation;

import com.feishu.miji.entity.OptimizationResult;
import com.feishu.miji.entity.Summary;
import com.feishu.miji.service.OptimizationCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 回调注册中心
 * 
 * 功能说明：
 * - 管理会话的回调订阅
 * - 支持多个回调订阅同一个会话
 * - 提供统一的回调触发入口
 */
@Slf4j
@Component
public class CallbackRegistry {
    
    /**
     * 会话回调列表映射
     * 每个会话可注册多个回调
     */
    private final Map<String, List<OptimizationCallback>> sessionCallbacks = new HashMap<>();
    
    /**
     * 全局回调列表
     * 所有会话都会触发的回调
     */
    private final List<OptimizationCallback> globalCallbacks = new CopyOnWriteArrayList<>();
    
    /**
     * 注册会话回调
     * 
     * @param sessionId 会话ID
     * @param callback 回调实例
     */
    public void register(String sessionId, OptimizationCallback callback) {
        if (sessionId == null || callback == null) {
            log.warn("注册回调失败：参数不能为空");
            return;
        }
        
        synchronized (sessionCallbacks) {
            sessionCallbacks.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>())
                    .add(callback);
        }
        
        log.debug("会话 {} 注册回调: {}", sessionId, callback.getCallbackName());
    }
    
    /**
     * 注销会话回调
     * 
     * @param sessionId 会话ID
     */
    public void unregister(String sessionId) {
        synchronized (sessionCallbacks) {
            List<OptimizationCallback> removed = sessionCallbacks.remove(sessionId);
            if (removed != null) {
                log.debug("会话 {} 注销 {} 个回调", sessionId, removed.size());
            }
        }
    }
    
    /**
     * 注销指定回调
     * 
     * @param sessionId 会话ID
     * @param callback 回调实例
     */
    public void unregister(String sessionId, OptimizationCallback callback) {
        synchronized (sessionCallbacks) {
            List<OptimizationCallback> callbacks = sessionCallbacks.get(sessionId);
            if (callbacks != null) {
                callbacks.remove(callback);
                log.debug("会话 {} 注销回调: {}", sessionId, callback.getCallbackName());
                
                if (callbacks.isEmpty()) {
                    sessionCallbacks.remove(sessionId);
                }
            }
        }
    }
    
    /**
     * 注册全局回调
     * 
     * @param callback 回调实例
     */
    public void registerGlobal(OptimizationCallback callback) {
        if (callback != null) {
            globalCallbacks.add(callback);
            log.info("注册全局回调: {}", callback.getCallbackName());
        }
    }
    
    /**
     * 注销全局回调
     * 
     * @param callback 回调实例
     */
    public void unregisterGlobal(OptimizationCallback callback) {
        if (callback != null) {
            globalCallbacks.remove(callback);
            log.info("注销全局回调: {}", callback.getCallbackName());
        }
    }
    
    /**
     * 触发优化完成回调
     * 
     * @param sessionId 会话ID
     * @param result 优化结果
     */
    public void triggerOptimizationComplete(String sessionId, OptimizationResult result) {
        // 先触发会话回调
        triggerSessionCallback(sessionId, callback -> {
            try {
                callback.onOptimizationComplete(result);
            } catch (Exception e) {
                log.error("会话 {} 回调 {} 异常", sessionId, callback.getCallbackName(), e);
            }
        });
        
        // 再触发全局回调
        triggerGlobalCallback(callback -> {
            try {
                callback.onOptimizationComplete(result);
            } catch (Exception e) {
                log.error("全局回调 {} 异常", callback.getCallbackName(), e);
            }
        });
    }
    
    /**
     * 触发优化失败回调
     * 
     * @param sessionId 会话ID
     * @param error 错误信息
     */
    public void triggerOptimizationError(String sessionId, String error) {
        triggerSessionCallback(sessionId, callback -> {
            try {
                callback.onOptimizationError(sessionId, error);
            } catch (Exception e) {
                log.error("会话 {} 错误回调 {} 异常", sessionId, callback.getCallbackName(), e);
            }
        });
        
        triggerGlobalCallback(callback -> {
            try {
                callback.onOptimizationError(sessionId, error);
            } catch (Exception e) {
                log.error("全局错误回调 {} 异常", callback.getCallbackName(), e);
            }
        });
    }
    
    /**
     * 触发摘要更新回调
     * 
     * @param sessionId 会话ID
     * @param summary 摘要
     */
    public void triggerSummaryUpdate(String sessionId, Summary summary) {
        triggerSessionCallback(sessionId, callback -> {
            try {
                callback.onSummaryUpdate(summary);
            } catch (Exception e) {
                log.error("会话 {} 摘要回调 {} 异常", sessionId, callback.getCallbackName(), e);
            }
        });
        
        triggerGlobalCallback(callback -> {
            try {
                callback.onSummaryUpdate(summary);
            } catch (Exception e) {
                log.error("全局摘要回调 {} 异常", callback.getCallbackName(), e);
            }
        });
    }
    
    /**
     * 触发会话结束回调
     * 
     * @param sessionId 会话ID
     * @param finalSummary 最终摘要
     */
    public void triggerSessionEnd(String sessionId, Summary finalSummary) {
        triggerSessionCallback(sessionId, callback -> {
            try {
                callback.onSessionEnd(sessionId, finalSummary);
            } catch (Exception e) {
                log.error("会话 {} 结束回调 {} 异常", sessionId, callback.getCallbackName(), e);
            }
        });
        
        triggerGlobalCallback(callback -> {
            try {
                callback.onSessionEnd(sessionId, finalSummary);
            } catch (Exception e) {
                log.error("全局结束回调 {} 异常", callback.getCallbackName(), e);
            }
        });
    }
    
    /**
     * 获取会话回调数量
     */
    public int getSessionCallbackCount(String sessionId) {
        List<OptimizationCallback> callbacks = sessionCallbacks.get(sessionId);
        return callbacks != null ? callbacks.size() : 0;
    }
    
    /**
     * 获取全局回调数量
     */
    public int getGlobalCallbackCount() {
        return globalCallbacks.size();
    }
    
    /**
     * 触发会话回调
     */
    private void triggerSessionCallback(String sessionId, CallbackRunnable runnable) {
        List<OptimizationCallback> callbacks;
        synchronized (sessionCallbacks) {
            callbacks = sessionCallbacks.get(sessionId);
        }
        
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        
        for (OptimizationCallback callback : callbacks) {
            runnable.run(callback);
        }
    }
    
    /**
     * 触发全局回调
     */
    private void triggerGlobalCallback(CallbackRunnable runnable) {
        for (OptimizationCallback callback : globalCallbacks) {
            runnable.run(callback);
        }
    }
    
    /**
     * 回调执行接口
     */
    @FunctionalInterface
    private interface CallbackRunnable {
        void run(OptimizationCallback callback);
    }
}
