# 自用版飞书妙记 - Spring Boot 后端

自用版飞书妙记项目的Spring Boot后端服务，提供音频转写、AI优化、实时总结功能。

## 项目结构

```
src/
├── main/
│   ├── java/com/voicenote/
│   │   ├── VoiceNoteApplication.java          # 应用入口
│   │   ├── config/                              # 配置类
│   │   │   ├── WebSocketConfig.java           # WebSocket配置
│   │   │   ├── WhisperProperties.java         # Whisper配置
│   │   │   ├── OllamaProperties.java          # Ollama配置
│   │   │   └── AudioProperties.java            # 音频配置
│   │   ├── controller/                         # REST控制器
│   │   │   ├── AudioController.java           # 音频接口
│   │   │   └── SessionController.java          # 会话接口
│   │   ├── dto/                                # 数据传输对象
│   │   │   ├── ApiResponse.java               # 统一响应
│   │   │   ├── ws/                             # WebSocket DTO
│   │   │   └── ...
│   │   ├── service/                            # 服务层
│   │   │   ├── SessionService.java            # 会话服务
│   │   │   ├── AudioTranscribeService.java    # 转写服务
│   │   │   └── TextOptimizeService.java       # 优化服务
│   │   ├── websocket/                          # WebSocket处理
│   │   │   ├── VoiceWebSocketHandler.java     # 音频处理器
│   │   │   └── VoiceHandshakeInterceptor.java # 握手拦截器
│   │   └── exception/                          # 异常处理
│   └── resources/
│       └── application.yml                    # 应用配置
└── test/                                       # 测试
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- Whisper (本地部署，端口8000)
- Ollama (本地部署，端口11434)

### 2. 构建项目

```bash
cd src/backend
mvn clean package
```

### 3. 运行服务

```bash
mvn spring-boot:run
```

### 4. 配置文件

编辑 `src/main/resources/application.yml` 配置Whisper和Ollama服务地址。

## API接口

### REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/session/create | 创建会话 |
| GET | /api/v1/session/{sessionId} | 查询会话 |
| POST | /api/v1/session/{sessionId}/end | 结束会话 |
| POST | /api/v1/audio/upload | 上传音频 |
| POST | /api/v1/audio/transcribe | 分段转写 |
| POST | /api/v1/text/optimize | 文本优化 |
| POST | /api/v1/summary/generate | 生成总结 |

### WebSocket

| URL | 说明 |
|-----|------|
| ws://localhost:8080/ws/voice | 实时音频流 |

## 技术栈

- Spring Boot 3.2.5
- Spring WebSocket
- Java 17
- Maven
