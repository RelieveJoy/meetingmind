package com.feishu.miji.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket音频数据消息
 * 
 * 客户端发送的音频帧数据结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioMessage {
    
    /** 消息类型: AUDIO */
    private String type;
    
    /** Base64编码的音频数据 */
    private String data;
    
    /** 采样率 */
    private Integer sampleRate;
    
    /** 通道数 */
    private Integer channels;
    
    /** 音频格式: PCM_16BIT */
    private String format;
    
    /** 是否GZIP压缩 */
    private Boolean compressed;
}
