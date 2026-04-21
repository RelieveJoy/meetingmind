package com.meetingmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama 服务配置属性类
 * 
 * 从 application.yml 中读取 ollama 相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {
    
    /**
     * Ollama 服务主机地址
     */
    private String host = "localhost";
    
    /**
     * Ollama 服务端口
     */
    private int port = 11434;
    
    /**
     * Ollama 服务完整 URL
     */
    private String baseUrl = "http://localhost:11434";
    
    /**
     * 总结生成模型
     */
    private String summaryModel = "qwen:7b";
    
    /**
     * 文本优化模型
     */
    private String optimizeModel = "qwen:7b";
    
    /**
     * 请求超时时间（秒）
     */
    private int timeout = 60;
}


