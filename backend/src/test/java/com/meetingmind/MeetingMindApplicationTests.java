package com.meetingmind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 应用上下文测试
 */
@SpringBootTest
@ActiveProfiles("test")
class MeetingMindApplicationTests {

    @Test
    void contextLoads() {
        // 验证应用上下文能够正常加载
    }
}


