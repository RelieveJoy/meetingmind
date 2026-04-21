package com.feishu.miji.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息传输对象
 * 
 * 用于前端与后端的实时通信
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    
    /**
     * 消息类型：
     * - transcription: 转写进度
     * - optimization: 优化进度
     * - summary: 摘要生成
     * - error: 错误信息
     * - complete: 完成通知
     */
    private String type;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 消息内容
     */
    private Object payload;
    
    /**
     * 进度百分比（0-100）
     */
    private int progress;
    
    /**
     * 状态：processing, success, error
     */
    private String status;
    
    /**
     * 错误信息（仅错误时）
     */
    private String errorMessage;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    /**
     * 创建转写消息
     */
    public static WebSocketMessage transcription(String sessionId, Object data, int progress) {
        return WebSocketMessage.builder()
                .type("transcription")
                .sessionId(sessionId)
                .payload(data)
                .progress(progress)
                .status("processing")
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建优化消息
     */
    public static WebSocketMessage optimization(String sessionId, Object data, int progress) {
        return WebSocketMessage.builder()
                .type("optimization")
                .sessionId(sessionId)
                .payload(data)
                .progress(progress)
                .status("processing")
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建完成消息
     */
    public static WebSocketMessage complete(String sessionId, Object data) {
        return WebSocketMessage.builder()
                .type("complete")
                .sessionId(sessionId)
                .payload(data)
                .progress(100)
                .status("success")
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误消息
     */
    public static WebSocketMessage error(String sessionId, String errorMessage) {
        return WebSocketMessage.builder()
                .type("error")
                .sessionId(sessionId)
                .errorMessage(errorMessage)
                .status("error")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
