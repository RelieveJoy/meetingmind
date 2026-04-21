package com.feishu.miji.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器
 *
 * 在WebSocket连接建立前进行参数验证。
 */
@Component
public class VoiceHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VoiceHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        // 获取sessionId参数
        String query = request.getURI().getQuery();
        String sessionId = null;
        if (query != null && query.contains("sessionId=")) {
            sessionId = query.split("sessionId=")[1].split("&")[0];
        }

        log.info("WebSocket握手: sessionId={}, uri={}", sessionId, request.getURI());

        // 将sessionId存储到WebSocket属性中
        attributes.put("sessionId", sessionId != null ? sessionId : "anonymous");

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // 握手完成后不需要特殊处理
    }
}