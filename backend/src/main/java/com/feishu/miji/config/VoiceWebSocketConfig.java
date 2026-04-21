package com.feishu.miji.config;

import com.feishu.miji.websocket.VoiceHandshakeInterceptor;
import com.feishu.miji.websocket.VoiceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 语音 WebSocket 配置类
 * 
 * 功能说明：
 * - 配置语音 WebSocket 处理器
 * - 映射 /ws/voice 路径
 * - 启用握手拦截器
 */
@Configuration
@EnableWebSocket
public class VoiceWebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private VoiceWebSocketHandler voiceWebSocketHandler;
    
    @Autowired
    private VoiceHandshakeInterceptor voiceHandshakeInterceptor;
    
    @Value("${websocket.endpoint:/ws/voice}")
    private String websocketEndpoint;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册语音 WebSocket 处理器
        registry.addHandler(voiceWebSocketHandler, websocketEndpoint)
                .addInterceptors(voiceHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
