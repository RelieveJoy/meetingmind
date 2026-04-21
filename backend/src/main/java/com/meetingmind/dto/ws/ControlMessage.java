package com.meetingmind.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket控制指令消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlMessage {
    
    /** 消息类型: CONTROL */
    private String type;
    
    /** 控制动作: START/PAUSE/RESUME/STOP */
    private String action;
    
    /** 时间戳 */
    private String timestamp;
}


