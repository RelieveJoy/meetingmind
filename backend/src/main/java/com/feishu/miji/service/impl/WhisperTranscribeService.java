package com.feishu.miji.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feishu.miji.config.WhisperProperties;
import com.feishu.miji.dto.TranscribeRequest;
import com.feishu.miji.dto.TranscribeResponse;
import com.feishu.miji.entity.AudioSegment;
import com.feishu.miji.entity.TranscriptionResult;
import com.feishu.miji.service.AudioTranscribeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Whisper音频转写服务实现
 * 
 * 集成Whisper语音识别API进行音频转写。
 */
@Service
public class WhisperTranscribeService implements AudioTranscribeService {
    
    private static final Logger log = LoggerFactory.getLogger(WhisperTranscribeService.class);
    
    @Autowired
    private WhisperProperties whisperProperties;

    @Lazy
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public TranscribeResponse transcribe(String sessionId, byte[] audioData, String language) {
        try {
            String whisperUrl = whisperProperties.getBaseUrl() + "/v1/audio/transcriptions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String audioBase64 = Base64.getEncoder().encodeToString(audioData);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", whisperProperties.getModel());
            requestBody.put("language", language);
            requestBody.put("audio", audioBase64);
            requestBody.put("response_format", "verbose_json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(whisperUrl, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response);

            return TranscribeResponse.builder()
                    .segmentId("seg_" + UUID.randomUUID().toString().substring(0, 8))
                    .sessionId(sessionId)
                    .text(jsonNode.path("text").asText(""))
                    .confidence(jsonNode.path("confidence").asDouble(0.0))
                    .transcribedAt(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            log.error("Whisper转写失败: {}", e.getMessage());
            throw new RuntimeException("Whisper转写失败: " + e.getMessage());
        }
    }
    
    @Override
    public TranscribeResponse transcribeStream(String sessionId, byte[] audioData) {
        return transcribe(sessionId, audioData, whisperProperties.getLanguage());
    }
    
    @Override
    public TranscribeResponse transcribeSegment(TranscribeRequest request) {
        return transcribe(request.getSessionId(), 
                request.getAudioData() != null ? Base64.getDecoder().decode(request.getAudioData()) : new byte[0],
                request.getLanguage());
    }
    
    /**
     * 异步转写音频分段
     */
    @Async
    public CompletableFuture<TranscriptionResult> transcribeAsync(AudioSegment segment) {
        try {
            log.info("开始异步转写分段: {}, 时长: {}s", segment.getSegmentId(),
                    segment.getEndTime() - segment.getStartTime());

            String whisperUrl = whisperProperties.getBaseUrl() + "/v1/audio/transcriptions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String audioBase64 = Base64.getEncoder().encodeToString(segment.getAudioData());

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", whisperProperties.getModel());
            requestBody.put("language", whisperProperties.getLanguage());
            requestBody.put("audio", audioBase64);
            requestBody.put("response_format", "verbose_json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(whisperUrl, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response);

            TranscriptionResult result = TranscriptionResult.builder()
                    .id("tr_" + UUID.randomUUID().toString().substring(0, 8))
                    .segmentId(segment.getSegmentId())
                    .sessionId(segment.getSessionId())
                    .originalText(jsonNode.path("text").asText(""))
                    .language(whisperProperties.getLanguage())
                    .startTime(segment.getStartTime())
                    .endTime(segment.getEndTime())
                    .confidence(jsonNode.path("confidence").asDouble(0.0))
                    .transcribedAt(LocalDateTime.now())
                    .build();

            log.info("异步转写完成: {}", segment.getSegmentId());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("异步转写失败: {}", segment.getSegmentId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 检查服务健康状态
     */
    public boolean isServiceHealthy() {
        try {
            String healthUrl = whisperProperties.getBaseUrl() + "/v1/models";
            HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());
            restTemplate.getForObject(healthUrl, String.class);
            return true;
        } catch (Exception e) {
            log.warn("Whisper服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
