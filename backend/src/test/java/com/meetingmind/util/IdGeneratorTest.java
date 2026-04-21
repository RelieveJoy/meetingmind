package com.meetingmind.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ID 生成器测试
 */
class IdGeneratorTest {

    @Test
    void generateUUIDShouldReturnNonNull() {
        String uuid = IdGenerator.generateUUID();
        assertNotNull(uuid);
        assertFalse(uuid.isEmpty());
    }

    @Test
    void generateSessionIdShouldStartWithPrefix() {
        String sessionId = IdGenerator.generateSessionId();
        assertTrue(sessionId.startsWith("sess_"));
    }

    @Test
    void generateSegmentIdShouldContainSessionId() {
        String sessionId = "test_session";
        int index = 5;
        String segmentId = IdGenerator.generateSegmentId(sessionId, index);
        
        assertTrue(segmentId.contains(sessionId));
        assertTrue(segmentId.contains("_seg_"));
        assertTrue(segmentId.contains("5"));
    }
}


