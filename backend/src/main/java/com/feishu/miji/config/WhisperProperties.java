package com.feishu.miji.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Whisper 服务配置属性类
 * 
 * 从 application.yml 中读取 whisper 相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "whisper")
public class WhisperProperties {
    
    /**
     * Whisper 服务主机地址
     */
    private String host = "localhost";
    
    /**
     * Whisper 服务端口
     */
    private int port = 5000;
    
    /**
     * Whisper 服务完整 URL
     */
    private String baseUrl = "http://localhost:5000";
    
    /**
     * 请求超时时间（秒）
     */
    private int timeout = 120;

    /**
     * 模型名称: tiny/base/small/medium/large
     */
    private String model = "small";

    /**
     * 默认语言
     */
    private String language = "zh";
}
