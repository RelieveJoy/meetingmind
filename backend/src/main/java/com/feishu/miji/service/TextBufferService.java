package com.feishu.miji.service;

import com.feishu.miji.entity.TextBuffer;
import com.feishu.miji.entity.TranscriptionResult;
import java.util.List;

public interface TextBufferService {
    /**
     * 添加转写文本到缓冲区
     * @param sessionId 会话ID
     * @param transcription 转写结果
     * @return 是否满足刷新条件
     */
    boolean addText(String sessionId, TranscriptionResult transcription);

    /**
     * 获取并清除满足条件的缓冲内容
     * @param sessionId 会话ID
     * @return 待优化的文本列表
     */
    List<TranscriptionResult> flushBuffer(String sessionId);

    /**
     * 获取缓冲区状态
     * @param sessionId 会话ID
     * @return 当前缓冲信息
     */
    TextBuffer.BufferStatus getBufferStatus(String sessionId);
}