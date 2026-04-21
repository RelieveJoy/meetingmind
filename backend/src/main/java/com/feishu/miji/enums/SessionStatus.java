package com.feishu.miji.enums;

/**
 * 会话状态枚举
 * 
 * 描述WebSocket会话的当前状态。
 */
public enum SessionStatus {
    
    /** 初始化状态 */
    INIT,
    
    /** 正在录音 */
    RECORDING,
    
    /** 暂停状态 */
    PAUSED,
    
    /** 已完成 */
    COMPLETED,
    
    /** 已取消 */
    CANCELLED
}
