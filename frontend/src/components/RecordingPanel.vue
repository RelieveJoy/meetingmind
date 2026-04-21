<template>
  <div class="recording-panel">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <div class="connection-status" :class="connectionStatus">
        {{ connectionStatusText }}
      </div>
      <div class="session-info">
        会话: {{ sessionId || '新会话' }}
      </div>
    </div>

    <!-- 音频波形可视化区域 -->
    <AudioVisualizer
      :audioData="audioData"
      :duration="duration"
      :isActive="status === 'recording'"
    />

    <!-- 实时转写文本区 -->
    <TranscriptDisplay
      :transcripts="transcriptList"
      :showOptimization="true"
    />

    <!-- 实时总结区 -->
    <SummaryCard
      :summary="summary"
      :isUpdating="isSummaryUpdating"
    />

    <!-- 录音控制区 -->
    <div class="control-area">
      <RecordButton
        :status="status"
        :disabled="connectionStatus === 'connecting' || connectionStatus === 'error'"
        @click="handleRecordButtonClick"
      />
    </div>

    <!-- 底部工具栏 -->
    <div class="toolbar">
      <button class="toolbar-btn" @click="exportTranscript">
        📥 导出
      </button>
      <button class="toolbar-btn" @click="clearTranscript">
        🗑️ 清空
      </button>
      <button class="toolbar-btn" @click="showHistory">
        📋 历史
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AudioVisualizer from './AudioVisualizer.vue'
import RecordButton from './RecordButton.vue'
import TranscriptDisplay from './TranscriptDisplay.vue'
import SummaryCard from './SummaryCard.vue'

// Props
const props = defineProps({
  sessionId: {
    type: String,
    default: null
  }
})

// Emits
const emit = defineEmits(['onTranscript', 'onSummary', 'onStatusChange'])

// 状态管理
const status = ref('idle') // idle, connecting, recording, paused, ended
const connectionStatus = ref('disconnected') // disconnected, connecting, connected, error
const sessionId = ref(props.sessionId || null)
const duration = ref(0)
const transcriptList = ref([])
const summary = ref({ summary: '', keyPoints: [] })
const isSummaryUpdating = ref(false)
const audioData = ref(null)
// 优化后的文本映射
const optimizedTexts = ref(new Map())

// WebSocket 连接
let websocket = null
let mediaRecorder = null
let audioContext = null
let analyser = null
let timer = null
let wsReconnectCount = 0
const WS_RECONNECT_MAX = 3

// 状态文本映射
const connectionStatusMap = {
  disconnected: '未连接',
  connecting: '连接中...',
  connected: '已连接',
  error: '连接异常'
}

const statusMap = {
  idle: '点击开始录音',
  connecting: '正在连接...',
  recording: '录音中',
  paused: '已暂停',
  ended: '录音结束'
}

// 计算属性
const connectionStatusText = computed(() => connectionStatusMap[connectionStatus.value])
const statusText = computed(() => statusMap[status.value])

// 初始化 WebSocket
const initWebSocket = () => {
  const WS_URL = 'ws://localhost:8080/ws/voice'
  websocket = new WebSocket(WS_URL)
  
  websocket.onopen = () => {
    connectionStatus.value = 'connected'
    wsReconnectCount = 0
    console.log('WebSocket 连接成功')
  }
  
  websocket.onmessage = (event) => {
    const message = JSON.parse(event.data)
    handleWebSocketMessage(message)
  }
  
  websocket.onclose = () => {
    connectionStatus.value = 'disconnected'
    if (wsReconnectCount < WS_RECONNECT_MAX) {
      wsReconnectCount++
      setTimeout(initWebSocket, 1000 * wsReconnectCount)
    }
  }
  
  websocket.onerror = () => {
    connectionStatus.value = 'error'
  }
}

// 处理 WebSocket 消息
const handleWebSocketMessage = (message) => {
  switch (message.type) {
    case 'TRANSCRIPT':
      if (message.data && message.data.segmentId && message.data.text) {
        const transcriptItem = {
          id: message.data.segmentId,
          text: message.data.text,
          optimizedText: message.data.optimizedText,
          startTime: message.data.startTime,
          endTime: message.data.endTime,
          confidence: message.data.confidence,
          isOptimized: !!message.data.optimizedText
        }
        transcriptList.value.push(transcriptItem)
        emit('onTranscript', transcriptItem)
      }
      break
    case 'SUMMARY_UPDATE':
      if (message.data && message.data.summary) {
        summary.value = {
          summaryId: message.data.summaryId,
          summary: message.data.summary,
          keyPoints: message.data.keyPoints || []
        }
        isSummaryUpdating.value = false
        emit('onSummary', summary.value)
      }
      break
    case 'SESSION_INFO':
      if (message.data && message.data.sessionId) {
        sessionId.value = message.data.sessionId
      }
      break
    case 'ERROR':
      // 处理不同格式的错误消息
      if (message.message) {
        console.error('WebSocket 错误:', message.message)
      } else if (message.data) {
        console.error('WebSocket 错误:', message.data)
      } else {
        console.error('WebSocket 错误:', message)
      }
      break
    case 'OPTIMIZATION_RESULT':
      // 用优化文本替换对应段落的原始文本
      if (message.data && message.data.segmentId && message.data.optimizedText) {
        const transcript = transcriptList.value.find(t => t.id === message.data.segmentId)
        if (transcript) {
          transcript.optimized = true
          transcript.optimizedText = message.data.optimizedText
        }
      }
      break
    default:
      console.log('未知 WebSocket 消息类型:', message.type)
  }
}

// 初始化录音
const initRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
    
    // 创建 AudioContext 用于可视化
    audioContext = new AudioContext({ sampleRate: 16000 })
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 2048
    
    const source = audioContext.createMediaStreamSource(stream)
    source.connect(analyser)
    
    // 创建 MediaRecorder
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
    
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0 && websocket && websocket.readyState === WebSocket.OPEN) {
        const reader = new FileReader()
        reader.onload = () => {
          const audioData = new Uint8Array(reader.result)
          // 转换为 Base64
          const base64Data = btoa(String.fromCharCode(...audioData))
          websocket.send(JSON.stringify({
            type: 'AUDIO',
            data: base64Data
          }))
        }
        reader.readAsArrayBuffer(event.data)
      }
    }
    
    mediaRecorder.onstop = () => {
      stream.getTracks().forEach(track => track.stop())
      if (audioContext) {
        audioContext.close()
      }
    }
    
    return stream
  } catch (error) {
    if (error.name === 'NotAllowedError') {
      alert('请允许访问麦克风')
    }
    console.error('初始化录音失败:', error)
    return null
  }
}

// 更新音频数据用于可视化
const updateAudioData = () => {
  if (analyser) {
    const dataArray = new Uint8Array(analyser.frequencyBinCount)
    analyser.getByteTimeDomainData(dataArray)
    audioData.value = dataArray
  }
  requestAnimationFrame(updateAudioData)
}

// 开始录音
const startRecording = async () => {
  status.value = 'connecting'
  
  // 初始化 WebSocket
  if (!websocket || websocket.readyState !== WebSocket.OPEN) {
    initWebSocket()
  }
  
  // 初始化录音
  const stream = await initRecording()
  if (!stream) {
    status.value = 'idle'
    return
  }
  
  // 发送开始消息
  if (websocket && websocket.readyState === WebSocket.OPEN) {
    websocket.send(JSON.stringify({
      type: 'CONTROL',
      action: 'START'
    }))
  }
  
  // 开始录音
  mediaRecorder.start(250) // 每250ms发送一次数据
  status.value = 'recording'
  duration.value = 0
  
  // 开始计时
  timer = setInterval(() => {
    duration.value++
  }, 1000)
  
  // 开始更新音频数据
  updateAudioData()
  
  emit('onStatusChange', { status: 'recording' })
}

// 暂停录音
const pauseRecording = () => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.pause()
    status.value = 'paused'
    clearInterval(timer)
    
    if (websocket && websocket.readyState === WebSocket.OPEN) {
      websocket.send(JSON.stringify({
        type: 'CONTROL',
        action: 'PAUSE'
      }))
    }
    
    emit('onStatusChange', { status: 'paused' })
  }
}

// 继续录音
const resumeRecording = () => {
  if (mediaRecorder && mediaRecorder.state === 'paused') {
    mediaRecorder.resume()
    status.value = 'recording'
    
    // 重启计时器
    timer = setInterval(() => {
      duration.value++
    }, 1000)
    
    if (websocket && websocket.readyState === WebSocket.OPEN) {
      websocket.send(JSON.stringify({
        type: 'CONTROL',
        action: 'RESUME'
      }))
    }
    
    emit('onStatusChange', { status: 'recording' })
  }
}

// 停止录音
const stopRecording = () => {
  if (mediaRecorder && (mediaRecorder.state === 'recording' || mediaRecorder.state === 'paused')) {
    mediaRecorder.stop()
    status.value = 'ended'
    clearInterval(timer)
    
    if (websocket && websocket.readyState === WebSocket.OPEN) {
      websocket.send(JSON.stringify({
        type: 'CONTROL',
        action: 'STOP'
      }))
    }
    
    emit('onStatusChange', { status: 'ended' })
  }
}

// 处理录音按钮点击
const handleRecordButtonClick = () => {
  switch (status.value) {
    case 'idle':
      startRecording()
      break
    case 'recording':
      pauseRecording()
      break
    case 'paused':
      resumeRecording()
      break
    case 'ended':
      // 重新开始
      status.value = 'idle'
      transcriptList.value = []
      summary.value = { summary: '', keyPoints: [] }
      duration.value = 0
      emit('onStatusChange', { status: 'idle' })
      break
  }
}

// 导出转写
const exportTranscript = () => {
  const content = `# 录音转写记录

**时间**: ${new Date().toISOString()}
**时长**: ${Math.floor(duration.value / 60)}:${(duration.value % 60).toString().padStart(2, '0')}

## 总结
${summary.value.summary || '无'}

### 关键点
${summary.value.keyPoints.map((point, index) => `${index + 1}. ${point}`).join('\n') || '无'}

## 转写内容
${transcriptList.value.map(item => `[${Math.floor(item.startTime / 60)}:${(item.startTime % 60).toString().padStart(2, '0')} - ${Math.floor(item.endTime / 60)}:${(item.endTime % 60).toString().padStart(2, '0')}] ${item.text}`).join('\n') || '无'}`
  
  const blob = new Blob([content], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `transcript_${new Date().toISOString().replace(/[:.]/g, '-')}.md`
  a.click()
  URL.revokeObjectURL(url)
}

// 清空转写
const clearTranscript = () => {
  if (confirm('确定要清空所有转写内容吗？')) {
    transcriptList.value = []
    summary.value = { summary: '', keyPoints: [] }
    duration.value = 0
  }
}

// 显示历史
const showHistory = () => {
  alert('历史功能开发中...')
}

// 生命周期
onMounted(() => {
  // 初始化 WebSocket
  initWebSocket()
})

onUnmounted(() => {
  // 清理资源
  if (websocket) {
    websocket.close()
  }
  if (mediaRecorder) {
    mediaRecorder.stop()
  }
  if (timer) {
    clearInterval(timer)
  }
  if (audioContext) {
    audioContext.close()
  }
})
</script>

<style scoped>
.recording-panel {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
}

.connection-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
}

.connection-status.disconnected {
  background: #ffecec;
  color: #d9534f;
}

.connection-status.connecting {
  background: #fff3cd;
  color: #f0ad4e;
}

.connection-status.connected {
  background: #e6f9e6;
  color: #5cb85c;
}

.connection-status.error {
  background: #ffecec;
  color: #d9534f;
}

.session-info {
  font-size: 14px;
  color: #666;
}

.control-area {
  display: flex;
  justify-content: center;
  margin: 30px 0;
}

.toolbar {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.toolbar-btn {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.toolbar-btn:hover {
  background: #f0f0f0;
  border-color: #999;
}

@media (max-width: 768px) {
  .recording-panel {
    padding: 10px;
  }
  
  .toolbar {
    gap: 10px;
  }
  
  .toolbar-btn {
    padding: 8px 16px;
    font-size: 12px;
  }
}
</style>