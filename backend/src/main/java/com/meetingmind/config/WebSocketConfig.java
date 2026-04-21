package com.meetingmind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 
 * 功能说明：
 * - 配置 WebSocket 消息代理
 * - 定义 STOMP 端点
 * - 启用消息广播和点对点通信
 * 
 * WebSocket 连接地址：ws://localhost:8080/api/ws
 * 订阅主题示例：/topic/transcription/{sessionId}
 */
@Configuration
@EnableWebSocketMessageBroker  // 启用 WebSocket 消息代理
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * - /topic: 用于广播消息（一对多）
     * - /queue: 用于点对点消息（一对一）
     * - /app: 用于服务端接收客户端消息
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理，用于广播
        config.enableSimpleBroker("/topic", "/queue");
        // 设置客户端发送消息的前缀（服务端接收地址）
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户特定消息的前缀
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 STOMP 端点
     * - 客户端通过此端点连接 WebSocket
     * - 启用 SockJS 提供降级支持
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 添加 STOMP 端点，支持跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 生产环境应限制具体域名
                .withSockJS();  // 启用 SockJS 降级
    }
}


