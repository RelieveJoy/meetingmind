package com.meetingmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * MeetingMind - 主启动类
 * 
 * 功能说明：
 * - 实时录音转文字
 * - 每40秒分段AI优化
 * - 实时总结
 * 
 * @author MeetingMind Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class MeetingMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingMindApplication.class, args);
        
        System.out.println("===========================================");
        System.out.println("  MeetingMind服务已启动");
        System.out.println("  访问地址: http://localhost:8080/api");
        System.out.println("  健康检查: http://localhost:8080/api/health");
        System.out.println("===========================================");
    }
}


