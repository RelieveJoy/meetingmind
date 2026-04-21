package com.feishu.miji.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feishu.miji.config.AudioProperties;
import com.feishu.miji.dto.GenerateSummaryRequest;
import com.feishu.miji.dto.OptimizeTextRequest;
import com.feishu.miji.dto.OptimizeTextResponse;
import com.feishu.miji.dto.ws.AudioMessage;
import com.feishu.miji.dto.ws.ControlMessage;
import com.feishu.miji.dto.ws.ErrorMessage;
import com.feishu.miji.dto.ws.TranscriptMessage;
import com.feishu.miji.enums.SessionStatus;
import com.feishu.miji.service.AudioTranscribeService;
import com.feishu.miji.service.SessionContextService;
import com.feishu.miji.service.SessionService;
import com.feishu.miji.service.TextOptimizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 语音WebSocket处理器
 * 
 * 处理实时音频流传输和转写结果推送。
 * 
 * 功能：
 * - 接收客户端发送的音频数据
 * - 调用Whisper进行实时转写
 * - 推送转写结果给客户端
 * - 40秒分段触发AI优化
 */
@Component
public class VoiceWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger log = LoggerFactory.getLogger(VoiceWebSocketHandler.class);
    
    /** 活动时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    /** 活动时间格式化器（带时区偏移） */
    private static final DateTimeFormatter FORMATTER_WITH_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    /**
     * 格式化时间
     */
    private String formatTimestamp() {
        try {
            return LocalDateTime.now().format(FORMATTER_WITH_OFFSET);
        } catch (Exception e) {
            // 降级使用简单格式
            return LocalDateTime.now().format(FORMATTER);
        }
    }
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private SessionContextService sessionContextService;
    
    @Autowired
    private AudioTranscribeService audioTranscribeService;
    
    @Autowired
    private TextOptimizeService textOptimizeService;
    
    @Autowired
    private AudioProperties audioProperties;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /** 活跃会话映射 */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    /** 音频块计数器 */
    private final Map<String, AtomicLong> audioChunkCounters = new ConcurrentHashMap<>();
    
    /** 录制开始时间 */
    private final Map<String, Long> recordingStartTimes = new ConcurrentHashMap<>();
    
    /** 转写文本缓冲 */
    private final Map<String, List<String>> transcriptBuffers = new ConcurrentHashMap<>();
    
    /** 段ID计数器 */
    private final Map<String, AtomicLong> segmentCounters = new ConcurrentHashMap<>();
    
    /**
     * WebSocket连接建立
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        log.info("WebSocket连接建立: sessionId={}", sessionId);
        
        // 存储会话
        sessions.put(sessionId, session);
        audioChunkCounters.put(sessionId, new AtomicLong(0));
        transcriptBuffers.put(sessionId, new CopyOnWriteArrayList<>());
        segmentCounters.put(sessionId, new AtomicLong(0));
        
        // 发送连接成功消息
        sendMessage(session, Map.of(
            "type", "CONNECTED",
            "sessionId", sessionId,
            "timestamp", formatTimestamp()
        ));
    }
    
    /**
     * 处理文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        String payload = message.getPayload();
        
        try {
            // 解析消息
            Map<String, Object> msgMap = objectMapper.readValue(payload, Map.class);
            String type = (String) msgMap.get("type");
            
            switch (type) {
                case "AUDIO":
                    handleAudioMessage(sessionId, msgMap);
                    break;
                    
                case "CONTROL":
                    handleControlMessage(sessionId, msgMap);
                    break;
                    
                case "PING":
                    // 心跳响应
                    sendMessage(session, Map.of(
                        "type", "PONG",
                        "timestamp", formatTimestamp()
                    ));
                    break;
                    
                default:
                    log.warn("未知消息类型: {}", type);
            }
            
        } catch (Exception e) {
            log.error("消息处理失败: {}", e.getMessage());
            sendError(session, 5000, "消息处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理音频数据消息
     */
    private void handleAudioMessage(String sessionId, Map<String, Object> msgMap) throws Exception {
        AudioMessage audioMsg = objectMapper.convertValue(msgMap, AudioMessage.class);
        
        // Base64解码音频数据
        byte[] audioData = java.util.Base64.getDecoder().decode(audioMsg.getData());
        
        // 添加到缓冲区
        sessionContextService.appendAudioBuffer(sessionId, audioData);
        
        // 增加计数器
        long chunkCount = audioChunkCounters.get(sessionId).incrementAndGet();
        
        // 调用Whisper进行转写
        try {
            var response = audioTranscribeService.transcribeStream(sessionId, audioData);
            
            // 添加到转写文本缓冲
            if (response.getText() != null && !response.getText().isEmpty()) {
                transcriptBuffers.get(sessionId).add(response.getText());
            }
            
            // 推送转写结果
            WebSocketSession wsSession = sessions.get(sessionId);
            if (wsSession != null && wsSession.isOpen()) {
                TranscriptMessage transcriptMsg = TranscriptMessage.builder()
                        .type("TRANSCRIPT")
                        .sessionId(sessionId)
                        .data(TranscriptMessage.TranscriptData.builder()
                                .segmentId(response.getSegmentId())
                                .text(response.getText())
                                .startTime(0.0)
                                .endTime((double) chunkCount * audioProperties.getFrameInterval() / 1000)
                                .confidence(response.getConfidence())
                                .isFinal(true)
                                .build())
                        .timestamp(formatTimestamp())
                        .build();
                
                wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(transcriptMsg)));
            }
            
        } catch (Exception e) {
            log.error("音频转写失败: {}", e.getMessage());
            WebSocketSession wsSession = sessions.get(sessionId);
            if (wsSession != null) {
                sendError(wsSession, 4000, "Whisper服务错误");
            }
        }
    }
    
    /**
     * 处理控制指令
     */
    private void handleControlMessage(String sessionId, Map<String, Object> msgMap) throws Exception {
        ControlMessage controlMsg = objectMapper.convertValue(msgMap, ControlMessage.class);
        String action = controlMsg.getAction();
        
        log.info("收到控制指令: sessionId={}, action={}", sessionId, action);
        
        WebSocketSession wsSession = sessions.get(sessionId);
        if (wsSession == null || !wsSession.isOpen()) {
            return;
        }
        
        switch (action) {
            case "START":
                sessionContextService.updateSessionStatus(sessionId, SessionStatus.RECORDING);
                recordingStartTimes.put(sessionId, System.currentTimeMillis());
                sendMessage(wsSession, Map.of(
                    "type", "CONTROL_ACK",
                    "action", "START",
                    "timestamp", formatTimestamp()
                ));
                break;
                
            case "PAUSE":
                sessionContextService.updateSessionStatus(sessionId, SessionStatus.PAUSED);
                sendMessage(wsSession, Map.of(
                    "type", "CONTROL_ACK",
                    "action", "PAUSE",
                    "timestamp", formatTimestamp()
                ));
                break;
                
            case "RESUME":
                sessionContextService.updateSessionStatus(sessionId, SessionStatus.RECORDING);
                sendMessage(wsSession, Map.of(
                    "type", "CONTROL_ACK",
                    "action", "RESUME",
                    "timestamp", formatTimestamp()
                ));
                break;
                
            case "STOP":
                sessionContextService.updateSessionStatus(sessionId, SessionStatus.COMPLETED);
                // 触发最终总结
                triggerFinalSummary(sessionId);
                sendMessage(wsSession, Map.of(
                    "type", "CONTROL_ACK",
                    "action", "STOP",
                    "timestamp", formatTimestamp()
                ));
                break;
                
            default:
                sendError(wsSession, 5001, "未知控制指令: " + action);
        }
    }
    
    /**
     * WebSocket连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session);
        log.info("WebSocket连接关闭: sessionId={}, status={}", sessionId, status);
        
        // 清理资源
        sessions.remove(sessionId);
        audioChunkCounters.remove(sessionId);
        recordingStartTimes.remove(sessionId);
    }
    
    /**
     * 定时任务：40秒分段优化
     * 
     * 每10秒检查一次是否需要触发分段优化
     */
    @Scheduled(fixedRate = 10000)
    public void triggerSegmentOptimization() {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            SessionStatus status = sessionContextService.getStatus(sessionId);
            
            if (status == SessionStatus.RECORDING) {
                // 检查录制时长
                Long startTime = recordingStartTimes.get(sessionId);
                if (startTime != null) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    long segmentInterval = audioProperties.getSegmentInterval();
                    
                    // 达到分段间隔，触发优化
                    if (elapsed > 0 && elapsed % segmentInterval == 0) {
                        triggerIntervalSummary(sessionId);
                    }
                }
            }
        }
    }
    
    /**
     * 触发阶段性总结和文字优化
     */
    private void triggerIntervalSummary(String sessionId) {
        try {
            // 从缓冲获取文本
            List<String> texts = transcriptBuffers.get(sessionId);
            if (texts == null || texts.isEmpty()) {
                return;
            }
            
            // 合并文本
            String transcript = String.join(" ", texts);
            
            // 获取并增加段ID
            long segmentId = segmentCounters.get(sessionId).incrementAndGet();
            
            // 优化文字
            OptimizeTextResponse optimizeResponse = textOptimizeService.optimizeText(
                    OptimizeTextRequest.builder()
                            .sessionId(sessionId)
                            .text(transcript)
                            .build());
            
            // 清空缓冲
            texts.clear();
            
            // 发送优化结果给前端
            WebSocketSession wsSession = sessions.get(sessionId);
            if (wsSession != null && wsSession.isOpen()) {
                // 发送 OPTIMIZATION_RESULT 消息
                Map<String, Object> optimizationMsg = Map.of(
                    "type", "OPTIMIZATION_RESULT",
                    "sessionId", sessionId,
                    "data", Map.of(
                        "segmentId", segmentId,
                        "optimizedId", optimizeResponse.getOptimizedId(),
                        "originalText", transcript,
                        "optimizedText", optimizeResponse.getOptimizedText()
                    ),
                    "timestamp", formatTimestamp()
                );
                wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(optimizationMsg)));
            }

            // 生成总结
            var summaryResponse = textOptimizeService.generateSummary(
                    GenerateSummaryRequest.builder()
                            .sessionId(sessionId)
                            .text(transcript)
                            .summaryType("brief")
                            .build());
            // 推送总结更新
            if (wsSession != null && wsSession.isOpen()) {
                Map<String, Object> summaryMsg = Map.of(
                    "type", "SUMMARY_UPDATE",
                    "sessionId", sessionId,
                    "data", Map.of(
                        "summaryId", summaryResponse.getSummaryId(),
                        "summary", summaryResponse.getContent(),
                        "keyPoints", summaryResponse.getKeyPoints()
                    ),
                    "timestamp", formatTimestamp()
                );
                wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(summaryMsg)));
            }
            
        } catch (Exception e) {
            log.error("阶段性总结和优化失败: {}", e.getMessage());
        }
    }
    
    /**
     * 触发最终总结
     */
    private void triggerFinalSummary(String sessionId) {
        try {
            String transcript = sessionContextService.getAndClearTranscript(sessionId);
            if (transcript == null || transcript.isEmpty()) {
                return;
            }

            var summaryResponse = textOptimizeService.generateSummary(
                    GenerateSummaryRequest.builder()
                            .sessionId(sessionId)
                            .text(transcript)
                            .summaryType("detailed")
                            .build());
            // 推送最终总结
            WebSocketSession wsSession = sessions.get(sessionId);
            if (wsSession != null && wsSession.isOpen()) {
                Map<String, Object> summaryMsg = Map.of(
                    "type", "FINAL_SUMMARY",
                    "sessionId", sessionId,
                    "data", Map.of(
                        "summaryId", summaryResponse.getSummaryId(),
                        "summary", summaryResponse.getContent(),
                        "keyPoints", summaryResponse.getKeyPoints()
                    ),
                    "timestamp", formatTimestamp()
                );
                wsSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(summaryMsg)));
            }
            
        } catch (Exception e) {
            log.error("最终总结失败: {}", e.getMessage());
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, int code, String message) {
        try {
            ErrorMessage errorMsg = ErrorMessage.builder()
                    .type("ERROR")
                    .code(code)
                    .message(message)
                    .timestamp(LocalDateTime.now().format(FORMATTER))
                    .build();
            
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
            }
        } catch (IOException e) {
            log.error("发送错误消息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从session中提取sessionId
     */
    private String extractSessionId(WebSocketSession session) {
        String uri = session.getUri() != null ? session.getUri().toString() : "";
        // 从查询参数中提取sessionId
        if (uri.contains("sessionId=")) {
            String[] parts = uri.split("sessionId=");
            if (parts.length > 1) {
                String id = parts[1].split("&")[0];
                return java.net.URLDecoder.decode(id, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        // 从path中提取
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] pathParts = path.split("/");
        if (pathParts.length > 0) {
            return pathParts[pathParts.length - 1];
        }
        return "anonymous";
    }
}
