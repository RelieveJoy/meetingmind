package com.meetingmind.service;

import com.meetingmind.entity.SessionContext;
import com.meetingmind.enums.SessionStatus;
import com.meetingmind.service.segmentation.SessionManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 会话上下文服务
 * 
 * 封装 SessionManagementService，提供会话上下文操作接口
 */
@Service
@RequiredArgsConstructor
public class SessionContextService {

    private final SessionManagementService sessionManagementService;

    /**
     * 获取会话上下文
     */
    public SessionContext getContext(String sessionId) {
        return sessionManagementService.getSession(sessionId);
    }

    /**
     * 更新会话状态
     */
    public void updateState(String sessionId, SessionContext.SessionState state) {
        SessionContext context = sessionManagementService.getSession(sessionId);
        if (context != null) {
            context.setState(state);
        }
    }

    /**
     * 添加转写文本
     */
    public void appendTranscript(String sessionId, String text) {
        SessionContext context = sessionManagementService.getSession(sessionId);
        if (context != null) {
            // 更新最后活动时间
            context.setLastActivityAt(java.time.LocalDateTime.now());
        }
    }

    /**
     * 添加音频数据到缓冲区
     *
     * @param sessionId 会话ID
     * @param audioData 音频数据
     */
    public void appendAudioBuffer(String sessionId, byte[] audioData) {
        SessionContext context = sessionManagementService.getSession(sessionId);
        if (context != null) {
            // 更新最后活动时间
            context.setLastActivityAt(LocalDateTime.now());

            // 更新音频缓冲状态
            if (context.getAudioBufferStatus() == null) {
                context.setAudioBufferStatus(SessionContext.AudioBufferStatus.builder()
                        .bufferedBytes(0L)
                        .bufferedSeconds(0.0)
                        .schedulerRunning(false)
                        .currentSegmentIndex(0)
                        .build());
            }

            SessionContext.AudioBufferStatus status = context.getAudioBufferStatus();
            status.setBufferedBytes(status.getBufferedBytes() + audioData.length);

            // 假设16kHz采样率，1字节=0.0000625秒
            Double additionalSeconds = (audioData.length / 32000.0);
            status.setBufferedSeconds(status.getBufferedSeconds() + additionalSeconds);
        }
    }

    /**
     * 获取并清除转写文本
     *
     * @param sessionId 会话ID
     * @return 转写文本
     */
    public String getAndClearTranscript(String sessionId) {
        SessionContext context = sessionManagementService.getSession(sessionId);
        if (context != null && context.getCompletedOptimizations() != null) {
            // 从已完成的优化结果中收集文本
            StringBuilder transcript = new StringBuilder();
            for (var opt : context.getCompletedOptimizations()) {
                if (opt.getOptimizedText() != null) {
                    transcript.append(opt.getOptimizedText()).append("\n");
                }
            }
            String result = transcript.toString();

            // 清除已完成的优化结果
            context.getCompletedOptimizations().clear();

            return result.trim();
        }
        return "";
    }

    /**
     * 更新会话状态
     *
     * @param sessionId 会话ID
     * @param status 会话状态
     */
    public void updateSessionStatus(String sessionId, SessionStatus status) {
        SessionContext.SessionState state = switch (status) {
            case INIT -> SessionContext.SessionState.CREATED;
            case RECORDING -> SessionContext.SessionState.RECORDING;
            case PAUSED -> SessionContext.SessionState.PAUSED;
            case COMPLETED, CANCELLED -> SessionContext.SessionState.ENDED;
        };
        updateState(sessionId, state);
    }

    /**
     * 获取会话状态
     *
     * @param sessionId 会话ID
     * @return 会话状态
     */
    public SessionStatus getStatus(String sessionId) {
        SessionContext context = sessionManagementService.getSession(sessionId);
        if (context == null || context.getState() == null) {
            return SessionStatus.INIT;
        }

        return switch (context.getState()) {
            case CREATED -> SessionStatus.INIT;
            case RECORDING -> SessionStatus.RECORDING;
            case PAUSED -> SessionStatus.PAUSED;
            case ENDED, ERROR -> SessionStatus.COMPLETED;
            case PROCESSING -> SessionStatus.RECORDING;
        };
    }
}


