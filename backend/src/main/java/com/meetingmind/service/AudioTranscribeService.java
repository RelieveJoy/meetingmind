package com.meetingmind.service;

import com.meetingmind.dto.TranscribeRequest;
import com.meetingmind.dto.TranscribeResponse;
import com.meetingmind.entity.AudioSegment;
import com.meetingmind.entity.TranscriptionResult;

import java.util.concurrent.CompletableFuture;

/**
 * 音频转写服务接口
 */
public interface AudioTranscribeService {
    
    /**
     * 转写音频数据
     */
    TranscribeResponse transcribe(String sessionId, byte[] audioData, String language);
    
    /**
     * 流式转写
     */
    TranscribeResponse transcribeStream(String sessionId, byte[] audioData);
    
    /**
     * 分段转写
     */
    TranscribeResponse transcribeSegment(TranscribeRequest request);
    
    /**
     * 异步转写音频分段
     */
    CompletableFuture<TranscriptionResult> transcribeAsync(AudioSegment segment);
    
    /**
     * 检查服务健康状态
     */
    boolean isServiceHealthy();
}


