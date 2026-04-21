package com.feishu.miji.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * 音频处理配置属性
 * 
 * 定义音频数据的处理参数，包括采样率、格式、分段间隔等。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "audio")
public class AudioProperties {
    
    /** 音频采样率 */
    private int sampleRate = 16000;
    
    /** 音频通道数 */
    private int channels = 1;
    
    /** 音频格式 */
    private String format = "PCM_16BIT";
    
    /** 每帧发送间隔(毫秒) */
    private int frameInterval = 250;
    
    /** 分段优化间隔(秒) */
    private int segmentInterval = 40;
    
    /** 是否启用GZIP压缩 */
    private boolean gzipCompression = true;
}
