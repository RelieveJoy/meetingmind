package com.meetingmind.controller;

import com.meetingmind.dto.ApiResponse;
import com.meetingmind.service.AudioTranscribeService;
import com.meetingmind.service.impl.OllamaTextOptimizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * 提供系统健康状态检查接口
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController implements HealthIndicator {
    
    private final AudioTranscribeService audioTranscribeService;
    private final OllamaTextOptimizeService ollamaTextOptimizeService;
    
    /**
     * 健康检查接口
     * 
     * @return 健康状态信息
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> check() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        // 系统基本信息
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("application", "meetingmind");
        healthInfo.put("version", "1.0.0");
        
        // 服务健康状态
        Map<String, Object> services = new HashMap<>();
        
        // Whisper 服务状态
        boolean whisperHealthy = audioTranscribeService.isServiceHealthy();
        services.put("whisper", Map.of(
                "status", whisperHealthy ? "UP" : "DOWN",
                "description", whisperHealthy ? "服务正常" : "服务不可用"
        ));
        
        // Ollama 服务状态
        boolean ollamaHealthy = ollamaTextOptimizeService.isServiceHealthy();
        services.put("ollama", Map.of(
                "status", ollamaHealthy ? "UP" : "DOWN",
                "description", ollamaHealthy ? "服务正常" : "服务不可用"
        ));
        
        healthInfo.put("services", services);
        
        // 总体状态
        boolean overallHealthy = whisperHealthy && ollamaHealthy;
        healthInfo.put("status", overallHealthy ? "UP" : "DEGRADED");
        
        log.debug("健康检查完成: {}", healthInfo);
        return ResponseEntity.ok(ApiResponse.success(healthInfo));
    }
    
    /**
     * 简洁健康检查（用于负载均衡）
     * 
     * @return UP 或 DOWN
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean healthy = audioTranscribeService.isServiceHealthy() 
                && ollamaTextOptimizeService.isServiceHealthy();
        
        if (healthy) {
            return ResponseEntity.ok("UP");
        } else {
            return ResponseEntity.status(503).body("DEGRADED");
        }
    }
    
    /**
     * 实现 HealthIndicator 接口
     */
    @Override
    public Health health() {
        boolean whisperHealthy = audioTranscribeService.isServiceHealthy();
        boolean ollamaHealthy = ollamaTextOptimizeService.isServiceHealthy();
        
        if (whisperHealthy && ollamaHealthy) {
            return Health.up()
                    .withDetail("whisper", "healthy")
                    .withDetail("ollama", "healthy")
                    .build();
        } else {
            return Health.down()
                    .withDetail("whisper", whisperHealthy ? "healthy" : "unhealthy")
                    .withDetail("ollama", ollamaHealthy ? "healthy" : "unhealthy")
                    .build();
        }
    }
}


