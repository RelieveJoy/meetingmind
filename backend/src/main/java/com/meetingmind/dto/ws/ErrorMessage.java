package com.meetingmind.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket错误消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorMessage {
    
    /** 消息类型: ERROR */
    private String type;
    
    /** 错误码 */
    private Integer code;
    
    /** 错误信息 */
    private String message;
    
    /** 时间戳 */
    private String timestamp;
}


