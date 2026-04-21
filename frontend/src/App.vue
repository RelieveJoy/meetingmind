<template>
  <div class="app">
    <header class="app-header">
      <h1>MeetingMind - 录音控制</h1>
      <p class="app-subtitle">实时音频转写与AI优化</p>
    </header>
    <main class="app-main">
      <div class="main-content">
        <RecordingPanel
          ref="recordingPanelRef"
          @onTranscript="handleTranscript"
          @onSummary="handleSummary"
          @onStatusChange="handleStatusChange"
        />
      </div>
      <aside class="sidebar-panel">
        <SummaryPanel
          ref="summaryPanelRef"
          layout="sidebar"
          :current-session="currentSession"
          :history-list="historyRecords"
          :show-history-tab="true"
          @export="handleExport"
          @view-history="handleViewHistory"
          @delete-history="handleDeleteHistory"
          @favorite="handleFavorite"
        >
          <template #header-extra>
            <button class="header-btn" @click="toggleSidebar" :title="showSidebar ? '收起侧边栏' : '展开侧边栏'">
              {{ showSidebar ? '✕' : '☰' }}
            </button>
          </template>
        </SummaryPanel>
      </aside>
    </main>
    <footer class="app-footer">
      <p>© 2026 MeetingMind</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import RecordingPanel from './components/RecordingPanel.vue'
import SummaryPanel from './components/SummaryPanel.vue'

// 引用
const recordingPanelRef = ref(null)
const summaryPanelRef = ref(null)

// 状态管理
const showSidebar = ref(true)
const historyRecords = ref([])

// 计算属性 - 从 RecordingPanel 获取当前会话数据
const currentSession = computed(() => recordingPanelRef.value ? {
  sessionId: recordingPanelRef.value.sessionId,
  summary: recordingPanelRef.value.summary,
  transcripts: recordingPanelRef.value.transcriptList,
  status: recordingPanelRef.value.status,
  duration: recordingPanelRef.value.duration
} : null)

// 处理转写事件
const handleTranscript = (transcript) => {
  console.log('收到转写:', transcript)
}

// 处理总结事件
const handleSummary = (summary) => {
  console.log('收到总结:', summary)
}

// 处理状态变更
const handleStatusChange = (status) => {
  console.log('状态变更:', status)
}

// 处理导出
const handleExport = ({ sessionId, format }) => {
  console.log('导出总结:', sessionId, format)
}

// 处理查看历史
const handleViewHistory = ({ sessionId }) => {
  console.log('查看历史:', sessionId)
}

// 处理删除历史
const handleDeleteHistory = ({ sessionId }) => {
  historyRecords.value = historyRecords.value.filter(r => r.sessionId !== sessionId)
  console.log('删除历史:', sessionId)
}

// 处理收藏
const handleFavorite = ({ sessionId, isFavorite }) => {
  const record = historyRecords.value.find(r => r.sessionId === sessionId)
  if (record) {
    record.isFavorite = isFavorite
  }
  console.log('收藏状态变更:', sessionId, isFavorite)
}

// 切换侧边栏显示
const toggleSidebar = () => {
  showSidebar.value = !showSidebar.value
}

// 生命周期
onMounted(() => {
  console.log('应用已挂载')
})

onUnmounted(() => {
  console.log('应用已卸载')
})
</script>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: #0f0f1a;
  color: #333;
}

/* 应用容器 */
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 头部 */
.app-header {
  background: #1a1a2e;
  color: white;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 4px;
}

.app-subtitle {
  font-size: 14px;
  opacity: 0.8;
}

/* 主内容区 */
.app-main {
  flex: 1;
  padding: 20px;
  display: flex;
  gap: 20px;
}

.main-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.sidebar-panel {
  width: 380px;
  flex-shrink: 0;
}

/* 侧边栏头部按钮 */
.header-btn {
  padding: 4px 8px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s ease-in-out;
}

.header-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
}

/* 底部 */
.app-footer {
  background: #1a1a2e;
  color: white;
  padding: 12px;
  text-align: center;
  font-size: 12px;
  opacity: 0.8;
  margin-top: auto;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .app-main {
    flex-direction: column;
  }

  .sidebar-panel {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .app-header h1 {
    font-size: 20px;
  }

  .app-main {
    padding: 10px;
  }
}
</style>
