package com.feishu.miji.util;

import lombok.experimental.UtilityClass;

import java.util.UUID;

/**
 * ID 生成工具类
 */
@UtilityClass
public class IdGenerator {
    
    /**
     * 生成 UUID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成会话ID
     */
    public static String generateSessionId() {
        return "sess_" + generateUUID();
    }
    
    /**
     * 生成分段ID
     */
    public static String generateSegmentId(String sessionId, int index) {
        return sessionId + "_seg_" + index;
    }
}
