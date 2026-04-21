package com.meetingmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 摘要服务配置属性类
 * 
 * 从 application.yml 中读取 summary 相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "summary")
public class SummaryProperties {
    
    /**
     * 触发条件配置
     */
    private TriggerConfig trigger = new TriggerConfig();
    
    /**
     * 模型配置
     */
    private ModelConfig model = new ModelConfig();
    
    /**
     * 上下文管理配置
     */
    private ContextConfig context = new ContextConfig();
    
    /**
     * 持久化配置
     */
    private PersistenceConfig persistence = new PersistenceConfig();
    
    /**
     * 触发条件配置
     */
    @Data
    public static class TriggerConfig {
        /**
         * 时间间隔（秒）
         */
        private int timeInterval = 120;
        
        /**
         * 文本量阈值（字数）
         */
        private int textThreshold = 500;
        
        /**
         * 最小触发间隔（秒）
         */
        private int minInterval = 30;
        
        /**
         * 最小文本量（字数）
         */
        private int minTextLength = 100;
    }
    
    /**
     * 模型配置
     */
    @Data
    public static class ModelConfig {
        /**
         * 使用的模型
         */
        private String name = "qwen:7b";
        
        /**
         * 超时时间（秒）
         */
        private int timeout = 60;
        
        /**
         * 生成温度
         */
        private double temperature = 0.7;
    }
    
    /**
     * 上下文管理配置
     */
    @Data
    public static class ContextConfig {
        /**
         * 最大历史摘要数
         */
        private int maxHistorySummaries = 5;
        
        /**
         * 最大上下文token估算
         */
        private int maxContextToken = 2000;
        
        /**
         * 压缩比例
         */
        private double compressionRatio = 0.3;
    }
    
    /**
     * 持久化配置
     */
    @Data
    public static class PersistenceConfig {
        /**
         * 是否启用持久化
         */
        private boolean enabled = true;
        
        /**
         * 存储路径
         */
        private String basePath = "./data/summaries";
        
        /**
         * 自动保存
         */
        private boolean autoSave = true;
    }
}

