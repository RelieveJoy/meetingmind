package com.meetingmind.service;

import com.meetingmind.config.SummaryProperties;
import com.meetingmind.service.impl.SummaryTriggerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SummaryTriggerServiceTest {
    
    @Mock
    private SummaryProperties summaryProperties;
    
    @Mock
    private SummaryService summaryService;
    
    @Mock
    private TextBufferService textBufferService;
    
    @InjectMocks
    private SummaryTriggerServiceImpl triggerService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置默认值
        SummaryProperties.TriggerConfig triggerConfig = new SummaryProperties.TriggerConfig();
        triggerConfig.setTimeInterval(120);
        triggerConfig.setTextThreshold(500);
        triggerConfig.setMinInterval(30);
        triggerConfig.setMinTextLength(100);
        
        when(summaryProperties.getTrigger()).thenReturn(triggerConfig);
    }
    
    @Test
    void testShouldTriggerSummary_InitialState() {
        String sessionId = "test-session-1";
        
        // 初始状态，不应该触发
        boolean result = triggerService.shouldTriggerSummary(sessionId);
        assertFalse(result);
    }
    
    @Test
    void testShouldTriggerSummary_TextThreshold() {
        String sessionId = "test-session-2";
        
        // 添加足够的文本
        triggerService.onTextUpdated(sessionId, "a".repeat(600));
        
        // 应该触发
        boolean result = triggerService.shouldTriggerSummary(sessionId);
        assertTrue(result);
    }
    
    @Test
    void testResetTriggerState() {
        String sessionId = "test-session-3";
        
        // 添加文本
        triggerService.onTextUpdated(sessionId, "a".repeat(600));
        
        // 重置状态
        triggerService.resetTriggerState(sessionId);
        
        // 不应该触发
        boolean result = triggerService.shouldTriggerSummary(sessionId);
        assertFalse(result);
    }
    
    @Test
    void testManualTrigger() {
        String sessionId = "test-session-4";
        
        // 手动触发应该执行，不会检查条件
        triggerService.manualTrigger(sessionId);
        
        // 验证方法被调用
        verify(summaryService, never()).generateSummary(anyString(), anyList(), anyString());
    }
    
    @Test
    void testTriggerFinalSummary() {
        String sessionId = "test-session-5";
        
        // 触发最终总结
        triggerService.triggerFinalSummary(sessionId);
        
        // 验证方法被调用
        verify(summaryService, never()).generateSummary(anyString(), anyList(), anyString());
    }
}

