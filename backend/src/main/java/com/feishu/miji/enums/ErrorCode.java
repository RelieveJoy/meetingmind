package com.feishu.miji.enums;

/**
 * 错误码枚举
 * 
 * 定义系统级、业务级、AI服务级错误码。
 */
public enum ErrorCode {
    
    // ==================== 系统级错误 (1xxx) ====================
    SYSTEM_ERROR(1000, "系统内部错误"),
    SERVICE_UNAVAILABLE(1001, "服务暂不可用"),
    REQUEST_TIMEOUT(1002, "请求超时"),
    PERMISSION_DENIED(1003, "权限不足"),
    
    // ==================== 参数错误 (2xxx) ====================
    PARAM_MISSING(2000, "参数缺失"),
    PARAM_FORMAT_ERROR(2001, "参数格式错误"),
    PARAM_OUT_OF_RANGE(2002, "参数值超出范围"),
    UNSUPPORTED_AUDIO_FORMAT(2003, "不支持的音频格式"),
    
    // ==================== 业务错误 (3xxx) ====================
    SESSION_NOT_FOUND(3000, "会话不存在"),
    SESSION_COMPLETED(3001, "会话已结束"),
    SEGMENT_NOT_FOUND(3002, "分段不存在"),
    TEXT_TOO_LONG(3003, "文本过长"),
    OPTIMIZE_TASK_QUEUED(3004, "优化任务排队中"),
    
    // ==================== AI服务错误 (4xxx) ====================
    WHISPER_SERVICE_ERROR(4000, "Whisper服务错误"),
    WHISPER_MODEL_LOAD_FAILED(4001, "Whisper模型加载失败"),
    OLLAMA_SERVICE_ERROR(4002, "Ollama服务错误"),
    OLLAMA_MODEL_NOT_FOUND(4003, "Ollama模型未找到"),
    AI_INFERENCE_TIMEOUT(4004, "AI推理超时"),
    
    // ==================== WebSocket错误 (5xxx) ====================
    WS_CONNECTION_FAILED(5000, "连接建立失败"),
    WS_INVALID_SESSION_ID(5001, "会话ID无效"),
    WS_CONNECTION_CLOSED(5002, "连接已断开"),
    WS_AUDIO_FORMAT_ERROR(5003, "音频数据格式错误"),
    WS_MESSAGE_TOO_LARGE(5004, "消息过大");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
