package com.meetingmind.controller;

import com.meetingmind.dto.*;
import com.meetingmind.entity.Summary;
import com.meetingmind.service.AudioTranscribeService;
import com.meetingmind.service.SummaryService;
import com.meetingmind.service.TextOptimizeService;
import com.meetingmind.service.impl.OllamaTextOptimizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 音频转写控制器
 */
@Slf4j
@RestController
@RequestMapping("/transcription")
@RequiredArgsConstructor
public class TranscriptionController {
    
    private final AudioTranscribeService audioTranscribeService;
    private final OllamaTextOptimizeService ollamaTextOptimizeService;
    private final SummaryService summaryService;
    
    // 模拟存储：生产环境应使用数据库
    private final ConcurrentHashMap<String, List<TranscribeResponse>> transcriptionStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<OptimizeTextResponse>> optimizationStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Summary> summaryStore = new ConcurrentHashMap<>();

    private List<TranscribeResponse> getTranscriptions(String sessionId) {
        return transcriptionStore.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }

    private List<OptimizeTextResponse> getOptimizations(String sessionId) {
        return optimizationStore.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }
    
    /**
     * 上传音频文件并转写
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TranscribeResponse>> uploadAndTranscribe(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "enableOptimization", defaultValue = "true") boolean enableOptimization,
            @RequestParam(value = "enableSummary", defaultValue = "false") boolean enableSummary) {
        
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("接收音频上传: sessionId={}, fileName={}, size={}", 
                sessionId, file.getOriginalFilename(), file.getSize());
        
        try {
            String filename = file.getOriginalFilename();
            String format = "wav";
            if (filename != null && filename.contains(".")) {
                format = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            }
            
            TranscribeResponse transcription = audioTranscribeService.transcribe(
                    sessionId,
                    file.getBytes(),
                    "zh"
            );

            getTranscriptions(sessionId).add(transcription);

            OptimizeTextResponse optimization = null;
            if (enableOptimization) {
                optimization = ollamaTextOptimizeService.optimizeText(
                        OptimizeTextRequest.builder()
                                .sessionId(sessionId)
                                .text(transcription.getText())
                                .segmentId(transcription.getSegmentId())
                                .build()
                );
                getOptimizations(sessionId).add(optimization);
            }

            Summary summary = null;
            if (enableSummary) {
                List<OptimizeTextResponse> optResults = getOptimizations(sessionId);
                String fullText = optResults.isEmpty()
                    ? (optimization != null ? optimization.getOptimizedText() : transcription.getText())
                    : String.join("\n", optResults.stream()
                        .map(OptimizeTextResponse::getOptimizedText)
                        .toList());
                summary = summaryService.generateSummaryFromText(sessionId, fullText);
                summaryStore.put(sessionId, summary);
            }
            
            String finalText = optimization != null ? optimization.getOptimizedText() : transcription.getText();

            TranscribeResponse response = TranscribeResponse.builder()
                    .segmentId(transcription.getSegmentId())
                    .transcriptionId(transcription.getTranscriptionId())
                    .sessionId(sessionId)
                    .text(finalText)
                    .speaker(transcription.getSpeaker())
                    .startTime(transcription.getStartTime())
                    .endTime(transcription.getEndTime())
                    .confidence(transcription.getConfidence())
                    .language(transcription.getLanguage())
                    .transcribedAt(transcription.getTranscribedAt())
                    .build();

            log.info("音频处理完成: sessionId={}", sessionId);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("音频处理失败: sessionId={}", sessionId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "音频处理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取会话转写结果
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<TranscribeResponse>>> getSessionTranscriptions(
            @PathVariable String sessionId) {

        List<TranscribeResponse> transcriptions = getTranscriptions(sessionId);
        List<OptimizeTextResponse> optimizations = getOptimizations(sessionId);

        Map<String, OptimizeTextResponse> optMap = optimizations.stream()
                .filter(o -> o.getSegmentId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        OptimizeTextResponse::getSegmentId,
                        o -> o,
                        (a, b) -> a
                ));

        List<TranscribeResponse> responses = transcriptions.stream()
                .map(t -> TranscribeResponse.builder()
                        .segmentId(t.getSegmentId())
                        .transcriptionId(t.getTranscriptionId())
                        .sessionId(sessionId)
                        .text(optMap.getOrDefault(t.getSegmentId(), null) != null
                                ? optMap.get(t.getSegmentId()).getOptimizedText()
                                : t.getText())
                        .speaker(t.getSpeaker())
                        .startTime(t.getStartTime())
                        .endTime(t.getEndTime())
                        .confidence(t.getConfidence())
                        .language(t.getLanguage())
                        .transcribedAt(t.getTranscribedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * 获取会话摘要
     */
    @GetMapping("/session/{sessionId}/summary")
    public ResponseEntity<ApiResponse<Summary>> getSessionSummary(
            @PathVariable String sessionId) {
        
        Summary summary = summaryStore.get(sessionId);
        if (summary == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "摘要不存在"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * 重新生成摘要
     */
    @PostMapping("/session/{sessionId}/summary/regenerate")
    public ResponseEntity<ApiResponse<Summary>> regenerateSummary(
            @PathVariable String sessionId) {

        List<OptimizeTextResponse> optimizations = getOptimizations(sessionId);
        if (optimizations.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "无可用文本数据"));
        }

        String fullText = optimizations.stream()
                .map(OptimizeTextResponse::getOptimizedText)
                .collect(java.util.stream.Collectors.joining("\n"));

        Summary summary = summaryService.generateSummaryFromText(sessionId, fullText);
        summaryStore.put(sessionId, summary);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * 清除会话数据
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<String>> clearSession(
            @PathVariable String sessionId) {
        
        transcriptionStore.remove(sessionId);
        optimizationStore.remove(sessionId);
        summaryStore.remove(sessionId);
        
        log.info("清除会话数据: sessionId={}", sessionId);
        return ResponseEntity.ok(ApiResponse.success("会话数据已清除"));
    }
}


