package com.feishu.miji.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文档配置属性
 * 
 * 用于配置文档生成相关的参数
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "document")
public class DocumentProperties {

    private String baseDir = "output";
    private String docDir = "documents";
    private String templateDir = "templates";
    private String tempDir = "temp";
    
    private TemplateConfig template = new TemplateConfig();
    private ExportConfig export = new ExportConfig();

    /**
     * 模板配置
     */
    @Data
    public static class TemplateConfig {
        private String defaultTemplate = "full_meeting";
        private String customTemplateDir = "custom";
    }

    /**
     * 导出配置
     */
    @Data
    public static class ExportConfig {
        private String defaultFormat = "markdown";
        private boolean includeTimestamp = true;
        private boolean includeAudio = true;
    }
}