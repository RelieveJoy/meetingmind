package com.meetingmind.controller;

import com.meetingmind.dto.ApiResponse;
import com.meetingmind.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 控制器
 * 
 * 处理 WebSocket 连接和消息
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    // 存储连接信息
    private final Map<String, String> sessionInfo = new ConcurrentHashMap<>();
    
    /**
     * 处理客户端发送的消息
     * 
     * @param message 消息内容
     * @param headerAccessor 消息头访问器
     * @return 响应消息
     */
    @MessageMapping("/audio")
    @SendToUser("/queue/reply")
    public WebSocketMessage handleAudioMessage(
            @Payload WebSocketMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = message.getSessionId();
        log.info("收到音频消息: sessionId={}, type={}", sessionId, message.getType());
        
        // TODO: 根据消息类型处理
        switch (message.getType()) {
            case "transcription" -> {
                // 处理转写请求
            }
            case "optimization" -> {
                // 处理优化请求
            }
            default -> {
                log.warn("未知消息类型: {}", message.getType());
            }
        }
        
        return WebSocketMessage.complete(sessionId, "处理完成");
    }
    
    /**
     * 处理连接事件
     */
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        String sessionId = event.getMessage().getHeaders().get("simpSessionId", String.class);
        if (sessionId != null) {
            sessionInfo.put(sessionId, "connected");
            log.info("WebSocket 连接建立: sessionId={}", sessionId);
        }
    }
    
    /**
     * 处理断开连接事件
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        sessionInfo.remove(sessionId);
        log.info("WebSocket 连接断开: sessionId={}", sessionId);
    }
    
    /**
     * 获取连接状态
     */
    @MessageMapping("/status")
    @SendToUser("/queue/status")
    public Map<String, String> getStatus() {
        return Map.of("status", "connected", "timestamp", String.valueOf(System.currentTimeMillis()));
    }
}


