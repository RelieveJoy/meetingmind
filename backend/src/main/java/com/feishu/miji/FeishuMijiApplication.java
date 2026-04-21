package com.feishu.miji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 飞书妙记 - 主启动类
 * 
 * 功能说明：
 * - 实时录音转文字
 * - 每40秒分段AI优化
 * - 实时总结
 * 
 * @author Feishu Miji Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync  // 启用异步处理支持
public class FeishuMijiApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(FeishuMijiApplication.class, args);
        
        System.out.println("===========================================");
        System.out.println("  飞书妙记服务已启动");
        System.out.println("  访问地址: http://localhost:8080/api");
        System.out.println("  健康检查: http://localhost:8080/api/health");
        System.out.println("===========================================");
    }
}
