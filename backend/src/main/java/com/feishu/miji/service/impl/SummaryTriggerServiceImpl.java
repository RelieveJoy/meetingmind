package com.feishu.miji.service.impl;

import com.feishu.miji.config.SummaryProperties;
import com.feishu.miji.service.SummaryService;
import com.feishu.miji.service.SummaryTriggerService;
import com.feishu.miji.service.TextBufferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 摘要触发服务实现类
 * 
 * 实现基于时间和文本量的触发逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryTriggerServiceImpl implements SummaryTriggerService {
    
    private final SummaryProperties summaryProperties;
    private final SummaryService summaryService;
    private final TextBufferService textBufferService;
    
    /**
     * 会话触发状态映射
     */
    private final Map<String, TriggerState> triggerStates = new ConcurrentHashMap<>();
    
    /**
     * 触发状态类
     */
    private static class TriggerState {
        private LocalDateTime lastSummaryTime;
        private int accumulatedTextLength;
        private LocalDateTime lastTextUpdateTime;
        
        public TriggerState() {
            this.lastSummaryTime = LocalDateTime.now();
            this.accumulatedTextLength = 0;
            this.lastTextUpdateTime = LocalDateTime.now();
        }
    }
    
    @Override
    public boolean shouldTriggerSummary(String sessionId) {
        TriggerState state = getOrCreateTriggerState(sessionId);
        
        // 检查最小间隔
        long secondsSinceLastSummary = java.time.Duration.between(
                state.lastSummaryTime, LocalDateTime.now()).getSeconds();
        if (secondsSinceLastSummary < summaryProperties.getTrigger().getMinInterval()) {
            return false;
        }
        
        // 检查文本量
        if (state.accumulatedTextLength < summaryProperties.getTrigger().getMinTextLength()) {
            return false;
        }
        
        // 时间触发
        if (secondsSinceLastSummary >= summaryProperties.getTrigger().getTimeInterval()) {
            log.info("会话 {} 触发摘要生成：时间触发", sessionId);
            return true;
        }
        
        // 文本量触发
        if (state.accumulatedTextLength >= summaryProperties.getTrigger().getTextThreshold()) {
            log.info("会话 {} 触发摘要生成：文本量触发", sessionId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void triggerSummary(String sessionId) {
        if (shouldTriggerSummary(sessionId)) {
            try {
                // 这里应该获取待总结的文本并调用 summaryService
                // 实际实现需要与 TextBufferService 集成
                log.info("执行摘要生成，会话ID: {}", sessionId);
                // TODO: 集成 TextBufferService 获取文本并调用 summaryService
                
                // 重置触发状态
                resetTriggerState(sessionId);
            } catch (Exception e) {
                log.error("触发摘要生成失败，会话ID: {}", sessionId, e);
            }
        }
    }
    
    @Override
    public void onTextUpdated(String sessionId, String newText) {
        TriggerState state = getOrCreateTriggerState(sessionId);
        state.accumulatedTextLength += newText.length();
        state.lastTextUpdateTime = LocalDateTime.now();
        
        // 检查是否需要触发
        triggerSummary(sessionId);
    }
    
    @Override
    public void resetTriggerState(String sessionId) {
        triggerStates.put(sessionId, new TriggerState());
        log.info("重置触发状态，会话ID: {}", sessionId);
    }
    
    @Override
    public void manualTrigger(String sessionId) {
        log.info("手动触发摘要生成，会话ID: {}", sessionId);
        // 直接触发，不检查条件
        try {
            // TODO: 集成 TextBufferService 获取文本并调用 summaryService
            resetTriggerState(sessionId);
        } catch (Exception e) {
            log.error("手动触发摘要生成失败，会话ID: {}", sessionId, e);
        }
    }
    
    @Override
    public void triggerFinalSummary(String sessionId) {
        log.info("触发最终摘要生成，会话ID: {}", sessionId);
        try {
            // TODO: 集成 TextBufferService 获取文本并调用 summaryService
            // 会话结束，清理状态
            triggerStates.remove(sessionId);
        } catch (Exception e) {
            log.error("触发最终摘要生成失败，会话ID: {}", sessionId, e);
        }
    }
    
    /**
     * 获取或创建触发状态
     */
    private TriggerState getOrCreateTriggerState(String sessionId) {
        return triggerStates.computeIfAbsent(sessionId, k -> new TriggerState());
    }
}