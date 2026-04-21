package com.feishu.miji.service;

import com.feishu.miji.dto.TranscribeRequest;
import com.feishu.miji.dto.TranscribeResponse;
import com.feishu.miji.entity.AudioSegment;
import com.feishu.miji.entity.TranscriptionResult;

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
