package com.voicenote;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 应用程序上下文测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "whisper.enabled=false",
    "ollama.enabled=false"
})
class VoiceNoteApplicationTests {

    @Test
    void contextLoads() {
        // 验证应用程序上下文能够正常加载
    }
}
