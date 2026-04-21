package com.feishu.miji.entity;

import java.util.List;

public class SessionTextBuffer {
    // 会话ID
    private String sessionId;

    // 当前缓冲的文本列表
    private List<TranscriptionResult> pendingTexts;

    // 缓冲配置
    private int maxSegments;      // 最大段数（默认3段）
    private int maxWaitSeconds;   // 最大等待秒数（默认60秒）

    // 缓冲状态
    private long lastFlushTime;   // 上次刷新时间
    private int segmentCount;     // 当前积累段数
}