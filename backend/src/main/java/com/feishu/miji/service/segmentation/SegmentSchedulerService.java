package com.feishu.miji.service.segmentation;

import com.feishu.miji.entity.AudioSegment;
import com.feishu.miji.service.AudioTranscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 分段定时调度服务
 * 
 * 功能说明：
 * - 为每个会话创建独立的定时任务
 * - 按固定间隔（40秒）触发音频分段处理
 * - 支持动态启停会话的定时器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentSchedulerService {
    
    /**
     * 分段时长（秒）
     */
    private static final int SEGMENT_DURATION = 40;
    
    /**
     * 首次执行延迟（秒）- 避免首段过短
     */
    private static final int INITIAL_DELAY = 5;
    
    /**
     * 会话定时器映射
     */
    private final Map<String, ScheduledFuture<?>> sessionSchedulers = new ConcurrentHashMap<>();
    
    /**
     * 会话上下文映射
     */
    private final Map<String, AudioBufferContext> audioBuffers = new ConcurrentHashMap<>();
    
    /**
     * 转写服务
     */
    private final AudioTranscribeService audioTranscribeService;
    
    /**
     * 音频缓冲区回调（分段后触发）
     */
    private SegmentProcessor segmentProcessor;
    
    /**
     * 调度器
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "segment-scheduler");
                t.setDaemon(true);
                return t;
            }
    );
    
    /**
     * 设置分段处理器
     */
    public void setSegmentProcessor(SegmentProcessor processor) {
        this.segmentProcessor = processor;
    }
    
    /**
     * 启动会话的分段定时器
     * 
     * @param sessionId 会话ID
     */
    public void startScheduler(String sessionId) {
        if (sessionSchedulers.containsKey(sessionId)) {
            log.warn("会话 {} 的定时器已启动，忽略重复请求", sessionId);
            return;
        }
        
        // 初始化音频缓冲区
        audioBuffers.put(sessionId, new AudioBufferContext());
        
        // 创建定时任务
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> executeSegmentTask(sessionId),
                INITIAL_DELAY,    // 首次延迟
                SEGMENT_DURATION, // 执行间隔
                TimeUnit.SECONDS
        );
        
        sessionSchedulers.put(sessionId, future);
        log.info("会话 {} 的分段定时器已启动，间隔 {} 秒", sessionId, SEGMENT_DURATION);
    }
    
    /**
     * 停止会话的分段定时器
     * 
     * @param sessionId 会话ID
     */
    public void stopScheduler(String sessionId) {
        ScheduledFuture<?> future = sessionSchedulers.remove(sessionId);
        if (future != null) {
            future.cancel(false);
            log.info("会话 {} 的分段定时器已停止", sessionId);
        }
        
        // 处理剩余音频数据
        processRemainingAudio(sessionId);
    }
    
    /**
     * 检查定时器是否运行中
     * 
     * @param sessionId 会话ID
     * @return 是否运行中
     */
    public boolean isRunning(String sessionId) {
        ScheduledFuture<?> future = sessionSchedulers.get(sessionId);
        return future != null && !future.isCancelled() && !future.isDone();
    }
    
    /**
     * 获取所有活跃会话数
     */
    public int getActiveSessionCount() {
        return (int) sessionSchedulers.values().stream()
                .filter(f -> !f.isCancelled() && !f.isDone())
                .count();
    }
    
    /**
     * 添加音频数据到缓冲区
     * 
     * @param sessionId 会话ID
     * @param audioData 音频数据
     */
    public void addAudioData(String sessionId, byte[] audioData) {
        AudioBufferContext buffer = audioBuffers.get(sessionId);
        if (buffer != null) {
            buffer.appendData(audioData);
        }
    }
    
    /**
     * 执行分段任务
     */
    private void executeSegmentTask(String sessionId) {
        try {
            AudioBufferContext buffer = audioBuffers.get(sessionId);
            if (buffer == null || buffer.isEmpty()) {
                log.debug("会话 {} 音频缓冲区为空，跳过本次分段", sessionId);
                return;
            }
            
            // 截取固定时长的音频数据
            AudioSegment segment = buffer.extractSegment(SEGMENT_DURATION);
            if (segment != null) {
                log.info("会话 {} 生成分段 #{}，时长 {:.1f}s", 
                        sessionId, segment.getIndex(), segment.getEndTime() - segment.getStartTime());
                
                // 调用分段处理器
                if (segmentProcessor != null) {
                    segmentProcessor.processSegment(segment);
                }
            }
            
        } catch (Exception e) {
            log.error("会话 {} 分段任务执行异常", sessionId, e);
        }
    }
    
    /**
     * 处理剩余音频数据（会话结束时）
     */
    private void processRemainingAudio(String sessionId) {
        AudioBufferContext buffer = audioBuffers.remove(sessionId);
        if (buffer != null && !buffer.isEmpty()) {
            AudioSegment segment = buffer.extractAll();
            if (segment != null && segmentProcessor != null) {
                log.info("会话 {} 处理剩余音频 {:.1f}s", 
                        sessionId, segment.getEndTime() - segment.getStartTime());
                segmentProcessor.processSegment(segment);
            }
        }
    }
    
    /**
     * 音频缓冲区上下文
     */
    private static class AudioBufferContext {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private Double currentTime = 0.0;
        private int segmentIndex = 0;
        private static final int SAMPLE_RATE = 16000; // Whisper 推荐采样率
        
        public synchronized void appendData(byte[] data) {
            buffer.write(data, 0, data.length);
            // 估算时间：字节数 / (采样率 * 2字节 * 单声道)
            currentTime += (float) data.length / (SAMPLE_RATE * 2);
        }
        
        public synchronized AudioSegment extractSegment(int durationSeconds) {
            // 计算需要的字节数
            int bytesNeeded = durationSeconds * SAMPLE_RATE * 2;
            if (buffer.size() < bytesNeeded) {
                return null;
            }
            
            byte[] audioData = new byte[bytesNeeded];
            byte[] allData = buffer.toByteArray();
            System.arraycopy(allData, 0, audioData, 0, bytesNeeded);
            
            // 重置缓冲区
            ByteArrayOutputStream newBuffer = new ByteArrayOutputStream();
            newBuffer.write(allData, bytesNeeded, allData.length - bytesNeeded);
            buffer.reset();
            try {
                buffer.write(newBuffer.toByteArray());
            } catch (java.io.IOException e) {
                log.error("音频缓冲区重置失败", e);
            }
            
            Double startTime = currentTime - (double) bytesNeeded / (SAMPLE_RATE * 2) - durationSeconds;
            Double endTime = startTime + durationSeconds;
            
            return AudioSegment.builder()
                    .segmentId(java.util.UUID.randomUUID().toString())
                    .index(segmentIndex++)
                    .startTime(startTime)
                    .endTime(endTime)
                    .audioData(audioData)
                    .format("wav")
                    .sampleRate(SAMPLE_RATE)
                    .createdAt(java.time.LocalDateTime.now())
                    .transcribed(false)
                    .optimized(false)
                    .build();
        }
        
        public synchronized AudioSegment extractAll() {
            if (buffer.size() == 0) {
                return null;
            }
            
            byte[] audioData = buffer.toByteArray();
            buffer.reset();
            
            Double duration = (double) audioData.length / (SAMPLE_RATE * 2);
            Double startTime = currentTime - duration;
            
            return AudioSegment.builder()
                    .segmentId(java.util.UUID.randomUUID().toString())
                    .index(segmentIndex++)
                    .startTime(startTime)
                    .endTime(currentTime)
                    .audioData(audioData)
                    .format("wav")
                    .sampleRate(SAMPLE_RATE)
                    .createdAt(java.time.LocalDateTime.now())
                    .transcribed(false)
                    .optimized(false)
                    .build();
        }
        
        public synchronized boolean isEmpty() {
            return buffer.size() == 0;
        }
    }
    
    /**
     * 分段处理器接口
     */
    @FunctionalInterface
    public interface SegmentProcessor {
        /**
         * 处理音频分段
         * @param segment 音频分段
         */
        void processSegment(AudioSegment segment);
    }
}
