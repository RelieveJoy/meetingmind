# MeetingMind - 实时音频转写与AI优化系统

![MeetingMind](frontend/src/assets/hero.png)

## 项目简介

MeetingMind是一个实时音频转写与AI优化系统，专为会议记录、语音笔记和实时字幕场景设计。该系统能够实时录制音频，将其转换为文本，并通过AI技术进行智能优化和总结，帮助用户更高效地管理和利用语音信息。

## 核心功能

### 🎙️ 实时音频录制与转写

- 支持实时麦克风录音
- 利用Whisper AI模型进行高精度语音识别
- 实时显示转写文本
- 音频波形可视化

### 🤖 AI文本优化

- 每40秒自动对录音片段进行AI优化
- 提升文本质量和可读性
- 修正语法和表达

### 📝 智能总结生成

- 基于录音内容自动生成会议总结
- 提取关键信息和要点
- 支持实时更新总结内容

### 📊 历史记录管理

- 保存录音和转写历史
- 支持查看和导出历史记录
- 收藏重要会话

### 📤 多格式导出

- 支持Markdown格式导出
- 支持Word文档导出
- 包含完整转写内容和总结

## 技术栈

### 后端

- **框架**: Spring Boot 3.2.5
- **语言**: Java 17
- **通信**: WebSocket (STOMP)
- **AI集成**: Whisper API, Ollama API
- **文档处理**: Apache POI
- **HTTP客户端**: OkHttp
- **构建工具**: Maven

### 前端

- **框架**: Vue 3
- **构建工具**: Vite
- **样式**: 原生CSS
- **WebSocket客户端**: 浏览器原生WebSocket API
- **音频处理**: Web Audio API

## 系统架构

```
┌─────────────────┐     WebSocket     ┌─────────────────┐
│                 │ 实时音频数据流    │                 │
│   前端应用      │──────────────────>│   后端服务      │
│ (Vue 3 + Vite)  │     文本/总结流   │ (Spring Boot)   │
│                 │<──────────────────│                 │
└─────────────────┘                   └─────────────────┘
                                          │
                                          ▼
┌─────────────────┐     HTTP请求     ┌─────────────────┐
│                 │<─────────────────│   后端服务      │
│   Whisper API   │  音频转写请求    │ (Spring Boot)   │
│ (语音识别)      │─────────────────>│                 │
└─────────────────┘     转写结果      └─────────────────┘
                                          │
                                          ▼
┌─────────────────┐     HTTP请求     ┌─────────────────┐
│                 │<─────────────────│   后端服务      │
│   Ollama API    │  文本优化/总结   │ (Spring Boot)   │
│ (AI模型)        │─────────────────>│                 │
└─────────────────┘     优化结果      └─────────────────┘
```

## 快速开始

### 前提条件

- Java 17 或更高版本
- Maven 3.6 或更高版本
- Node.js 14 或更高版本
- npm 或 yarn
- Whisper API 服务 (本地部署或API密钥)
- Ollama 服务 (本地部署)

### 安装步骤

#### 1. 克隆项目

```bash
git clone <repository-url>
cd meetingmind
```

#### 2. 配置后端服务

1. 编辑 `backend/src/main/resources/application.yml` 文件，配置以下内容：

```yaml
# Whisper 服务配置
whisper:
  enabled: true
  base-url: http://localhost:8000  # Whisper API地址
  model: small  # 模型大小
  language: zh  # 默认语言

# Ollama 服务配置
ollama:
  enabled: true
  base-url: http://localhost:11434  # Ollama API地址
  optimize-model: chatglm3:6b-128k  # 文本优化模型
  summary-model: chatglm3:6b-128k  # 总结生成模型
```

1. 构建并启动后端服务：

```bash
cd backend
mvn clean package
# 或使用启动脚本
./start.sh  # Linux/Mac
# 或
start.bat  # Windows
```

#### 3. 配置前端应用

1. 安装依赖：

```bash
cd frontend
npm install
```

1. 启动开发服务器：

```bash
npm run dev
```

### 访问应用

- 前端应用：`http://localhost:5173`
- 后端API：`http://localhost:8080/api`
- 健康检查：`http://localhost:8080/api/health`

## 使用指南

### 1. 开始录音

1. 打开前端应用
2. 点击录音按钮开始录制
3. 系统会自动连接WebSocket并开始转写

### 2. 查看转写和总结

- 实时转写文本会显示在主界面
- AI优化后的文本会自动更新
- 实时总结会显示在侧边栏

### 3. 导出内容

1. 点击导出按钮
2. 选择导出格式 (Markdown)
3. 系统会生成并下载导出文件

### 4. 管理历史记录

- 在侧边栏查看历史会话
- 点击历史记录查看详细内容
- 可以删除或收藏历史记录

## 配置说明

### 后端配置

主要配置文件：`backend/src/main/resources/application.yml`

- **服务器配置**: 端口、上下文路径
- **Whisper配置**: API地址、模型、语言
- **Ollama配置**: API地址、模型选择
- **音频处理**: 采样率、通道数、格式
- **WebSocket配置**: 端点、心跳间隔
- **分段优化**: 分段时长、并发数
- **总结配置**: 触发条件、模型参数
- **文档生成**: 输出目录、模板配置

### 前端配置

主要配置文件：`frontend/vite.config.js`

- **开发服务器**: 端口、代理设置
- **构建配置**: 输出目录、资产路径

## 项目结构

```
meetingmind/
├── backend/                # 后端服务
│   ├── src/main/java/com/meetingmind/  # Java源代码
│   │   ├── config/         # 配置类
│   │   ├── controller/     # 控制器
│   │   ├── dto/            # 数据传输对象
│   │   ├── entity/         # 实体类
│   │   ├── enums/          # 枚举类
│   │   ├── exception/      # 异常处理
│   │   ├── service/        # 服务层
│   │   ├── util/           # 工具类
│   │   ├── websocket/      # WebSocket处理
│   │   └── MeetingMindApplication.java  # 主应用类
│   ├── src/main/resources/ # 资源文件
│   │   └── application.yml # 配置文件
│   ├── pom.xml             # Maven配置
│   └── start.sh/start.bat  # 启动脚本
├── frontend/               # 前端应用
│   ├── public/             # 静态资源
│   ├── src/                # 源代码
│   │   ├── assets/         # 资源文件
│   │   ├── components/     # Vue组件
│   │   ├── App.vue         # 主应用组件
│   │   └── main.js         # 入口文件
│   ├── package.json        # npm配置
│   └── vite.config.js      # Vite配置
└── README.md               # 项目说明
```

## 核心API

### 后端API

#### 健康检查

- `GET /api/health` - 检查服务健康状态

#### 音频转写

- `POST /api/transcribe` - 提交音频文件进行转写

#### 文档生成

- `POST /api/document/generate` - 生成会议文档
- `GET /api/document/templates` - 获取可用模板

### WebSocket端点

- `ws://localhost:8080/ws/voice` - 音频流和实时转写通道

#### 消息类型

- `AUDIO` - 音频数据
- `CONTROL` - 控制命令 (START/PAUSE/RESUME/STOP)
- `TRANSCRIPT` - 转写结果
- `SUMMARY_UPDATE` - 总结更新
- `ERROR` - 错误信息

## 技术亮点

1. **实时处理**：通过WebSocket实现音频流的实时传输和处理
2. **AI集成**：无缝集成Whisper和Ollama AI模型
3. **分段优化**：智能分段处理，提高系统响应速度
4. **多格式导出**：支持Markdown和Word格式导出
5. **用户友好**：直观的界面设计和流畅的用户体验
6. **可扩展性**：模块化设计，易于添加新功能
7. **性能优化**：异步处理和并发控制，提高系统性能

## 部署说明

### 开发环境

1. 启动Whisper服务：`whisper-server --port 8000`
2. 启动Ollama服务：`ollama serve`
3. 启动后端服务：`./start.sh` 或 `start.bat`
4. 启动前端服务：`npm run dev`

### 生产环境

1. 构建前端：`npm run build`
2. 构建后端：`mvn clean package`
3. 部署后端jar包：`java -jar backend/target/meetingmind-backend-1.0.0.jar`
4. 部署前端静态文件：使用Nginx或其他Web服务器

## 故障排除

### 常见问题

1. **WebSocket连接失败**
   - 检查后端服务是否启动
   - 检查网络连接和防火墙设置
2. **转写失败**
   - 检查Whisper服务是否正常运行
   - 检查音频权限是否正确
3. **总结生成失败**
   - 检查Ollama服务是否正常运行
   - 检查模型是否正确加载
4. **导出失败**
   - 检查文件系统权限
   - 检查存储空间是否充足

## 未来计划

- [ ] 支持多语言转写
- [ ] 添加 speaker 识别功能
- [ ] 实现云端存储和同步
- [ ] 开发移动应用
- [ ] 增加更多AI模型选择
- [ ] 支持视频会议集成

## 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

## 许可证

MIT License

## 联系我们

- 项目团队: MeetingMind Team
- 版本: 1.0.0
- 日期: 2026-04-21

***

**MeetingMind** - 让语音记录更智能，让会议管理更高效！
