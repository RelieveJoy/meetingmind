<template>
  <div class="summary-panel" :class="[`layout-${layout}`, { collapsed: isCollapsed, 'no-history': !showHistoryTab }]">
    <!-- Header 区域 -->
    <div class="panel-header">
      <div class="header-left">
        <h2 class="panel-title">总结面板</h2>
        <span v-if="currentSession && currentSession.status" class="status-badge" :class="currentSession.status">
          {{ statusTextMap[currentSession.status] || '未知' }}
        </span>
      </div>
      <div class="header-right">
        <slot name="header-extra"></slot>
        <button v-if="layout === 'sidebar'" class="collapse-btn" @click="toggleCollapse" :title="isCollapsed ? '展开' : '收起'">
          {{ isCollapsed ? '»' : '«' }}
        </button>
      </div>
    </div>

    <!-- Tab Bar -->
    <div v-if="showHistoryTab" class="tab-bar">
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'current' }"
        @click="activeTab = 'current'"
      >
        当前会话
      </button>
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'history' }"
        @click="switchToHistory"
      >
        历史记录
        <span v-if="historyCount > 0" class="badge">{{ historyCount }}</span>
      </button>
    </div>

    <!-- Content Area -->
    <div class="panel-content" v-show="!isCollapsed">
      <!-- 当前会话内容 -->
      <div v-if="activeTab === 'current'" class="current-session">
        <!-- Summary Card -->
        <div class="summary-card">
          <div class="card-header">
            <h3 class="card-title">实时总结</h3>
            <span v-if="currentSession" class="session-duration">
              {{ formatDuration(currentSession.duration) }}
            </span>
          </div>
          <div class="card-content">
            <!-- 加载状态 -->
            <div v-if="isSummaryUpdating" class="loading-state">
              <div class="loading-spinner"></div>
              <span>AI 正在生成总结...</span>
            </div>
            <!-- 总结内容 -->
            <div v-else-if="displaySummary" class="summary-content">
              <!-- 关键点标签 -->
              <div v-if="displaySummary.keyPoints && displaySummary.keyPoints.length > 0" class="key-points">
                <span 
                  v-for="(point, index) in displaySummary.keyPoints" 
                  :key="index"
                  class="key-point-tag"
                >
                  📌 {{ point }}
                </span>
              </div>
              <!-- 总结正文 -->
              <div class="summary-text" :class="{ expanded: isSummaryExpanded }">
                {{ displaySummary.summary }}
              </div>
              <button class="expand-btn" @click="isSummaryExpanded = !isSummaryExpanded">
                {{ isSummaryExpanded ? '收起' : '展开完整总结' }} {{ isSummaryExpanded ? '▲' : '▼' }}
              </button>
            </div>
            <!-- 空状态 -->
            <div v-else class="empty-state">
              开始录音后 AI 将生成实时总结
            </div>
          </div>
          <div class="card-footer">
            <span class="update-time" v-if="displaySummary && displaySummary.updatedAt">
              更新于 {{ formatTime(displaySummary.updatedAt) }}
            </span>
          </div>
        </div>

        <!-- Transcript Reference -->
        <div v-if="showTranscriptRef && currentSession && currentSession.transcripts && currentSession.transcripts.length > 0" class="transcript-ref">
          <button class="transcript-toggle" @click="isTranscriptExpanded = !isTranscriptExpanded">
            <span>转写参考</span>
            <span class="transcript-count">({{ currentSession.transcripts.length }} 段)</span>
            <span class="toggle-icon">{{ isTranscriptExpanded ? '▲' : '▼' }}</span>
          </button>
          <div v-show="isTranscriptExpanded" class="transcript-list">
            <div 
              v-for="item in currentSession.transcripts" 
              :key="item.id"
              class="transcript-item"
            >
              <span class="transcript-time">[{{ formatTime(item.startTime) }}]</span>
              <span class="transcript-text">{{ item.text }}</span>
            </div>
          </div>
        </div>

        <slot name="summary-footer"></slot>
      </div>

      <!-- 历史记录内容 -->
      <div v-else-if="activeTab === 'history'" class="history-records">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索历史记录..."
            class="search-input"
          />
        </div>

        <!-- 历史记录列表 -->
        <div class="history-list">
          <div 
            v-for="record in filteredHistoryList" 
            :key="record.sessionId"
            class="history-item"
            :class="{ selected: selectedHistoryId === record.sessionId }"
            @click="selectHistoryRecord(record)"
          >
            <div class="history-header">
              <span class="history-date">{{ formatDate(record.startTime) }}</span>
              <div class="history-actions" v-if="!readonly">
                <button class="action-btn" @click.stop="toggleFavorite(record)" :title="record.isFavorite ? '取消收藏' : '收藏'">
                  {{ record.isFavorite ? '⭐' : '☆' }}
                </button>
                <button class="action-btn" @click.stop="exportRecord(record)" title="导出">📥</button>
                <button class="action-btn delete" @click.stop="deleteRecord(record)" title="删除">🗑️</button>
              </div>
            </div>
            <div class="history-title">{{ record.title || '无标题' }}</div>
            <div class="history-meta">
              <span>时长: {{ formatDuration(record.duration) }}</span>
              <span v-if="record.summary && record.summary.keyPoints">关键点: {{ record.summary.keyPoints.length }}</span>
            </div>
            <div v-if="record.summary && record.summary.content" class="history-preview">
              {{ truncateText(record.summary.content, 100) }}
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="filteredHistoryList.length === 0" class="empty-history">
            <slot name="empty-history">
              <div class="empty-icon">📋</div>
              <div class="empty-text">{{ searchQuery ? '未找到匹配的历史记录' : '暂无历史记录' }}</div>
            </slot>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer 操作按钮 -->
    <div v-if="!readonly && !isCollapsed && activeTab === 'current'" class="panel-footer">
      <button class="footer-btn" @click="handleExport" title="导出总结">
        📥 导出
      </button>
      <button class="footer-btn" @click="handleCopy" title="复制总结">
        📋 复制
      </button>
      <button v-if="currentSession" class="footer-btn" @click="handleFavorite" :class="{ active: isCurrentFavorite }" title="收藏">
        {{ isCurrentFavorite ? '⭐' : '☆' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, provide, inject } from 'vue'

// DB_NAME 和 STORE_NAME 常量
const DB_NAME = 'SummaryHistoryDB'
const STORE_NAME = 'historyRecords'
const DB_VERSION = 1

// Props
const props = defineProps({
  layout: {
    type: String,
    default: 'panel',
    validator: (value) => ['sidebar', 'panel', 'embedded'].includes(value)
  },
  defaultExpanded: {
    type: Boolean,
    default: true
  },
  showHistoryTab: {
    type: Boolean,
    default: true
  },
  currentSession: {
    type: Object,
    default: null
  },
  historyList: {
    type: Array,
    default: () => []
  },
  readonly: {
    type: Boolean,
    default: false
  },
  showTranscriptRef: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['export', 'deleteHistory', 'favorite', 'viewHistory', 'collapse'])

// 状态
const activeTab = ref('current')
const isCollapsed = ref(!props.defaultExpanded)
const isSummaryExpanded = ref(false)
const isTranscriptExpanded = ref(false)
const isSummaryUpdating = ref(false)
const searchQuery = ref('')
const selectedHistoryId = ref(null)
const historyCount = ref(0)
const isCurrentFavorite = ref(false)

// IndexedDB 相关
let db = null

// 状态文本映射
const statusTextMap = {
  idle: '空闲',
  connecting: '连接中',
  recording: '录音中',
  paused: '已暂停',
  ended: '已结束'
}

// 计算属性：显示的总结内容
const displaySummary = computed(() => {
  if (props.currentSession && props.currentSession.summary) {
    return props.currentSession.summary
  }
  return null
})

// 计算属性：过滤后的历史记录列表
const filteredHistoryList = computed(() => {
  if (!searchQuery.value) {
    return props.historyList
  }
  const query = searchQuery.value.toLowerCase()
  return props.historyList.filter(record => {
    const titleMatch = record.title && record.title.toLowerCase().includes(query)
    const contentMatch = record.summary && record.summary.content && record.summary.content.toLowerCase().includes(query)
    return titleMatch || contentMatch
  })
})

// 切换收起/展开
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  emit('collapse', { isCollapsed: isCollapsed.value })
}

// 切换到历史记录标签
const switchToHistory = () => {
  activeTab.value = 'history'
  emit('viewHistory', { sessionId: props.currentSession?.sessionId })
}

// 选择历史记录
const selectHistoryRecord = (record) => {
  selectedHistoryId.value = record.sessionId
  emit('viewHistory', { sessionId: record.sessionId })
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds && seconds !== 0) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 格式化时间戳
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const seconds = date.getSeconds().toString().padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

// 截断文本
const truncateText = (text, maxLength) => {
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

// 初始化 IndexedDB
const initDB = () => {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)
    
    request.onerror = () => {
      console.error('IndexedDB 打开失败')
      reject(request.error)
    }
    
    request.onsuccess = () => {
      db = request.result
      resolve(db)
    }
    
    request.onupgradeneeded = (event) => {
      const database = event.target.result
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        const objectStore = database.createObjectStore(STORE_NAME, { keyPath: 'sessionId' })
        objectStore.createIndex('startTime', 'startTime', { unique: false })
        objectStore.createIndex('isFavorite', 'isFavorite', { unique: false })
      }
    }
  })
}

// 获取所有历史记录
const getAllHistoryRecords = () => {
  return new Promise((resolve, reject) => {
    if (!db) {
      resolve([])
      return
    }
    const transaction = db.transaction([STORE_NAME], 'readonly')
    const objectStore = transaction.objectStore(STORE_NAME)
    const request = objectStore.getAll()
    
    request.onsuccess = () => {
      resolve(request.result || [])
    }
    
    request.onerror = () => {
      console.error('获取历史记录失败')
      reject(request.error)
    }
  })
}

// 保存历史记录
const saveHistoryRecord = (record) => {
  return new Promise((resolve, reject) => {
    if (!db) {
      reject(new Error('数据库未初始化'))
      return
    }
    const transaction = db.transaction([STORE_NAME], 'readwrite')
    const objectStore = transaction.objectStore(STORE_NAME)
    const request = objectStore.put(record)
    
    request.onsuccess = () => {
      resolve()
    }
    
    request.onerror = () => {
      console.error('保存历史记录失败')
      reject(request.error)
    }
  })
}

// 删除历史记录
const deleteHistoryRecord = (sessionId) => {
  return new Promise((resolve, reject) => {
    if (!db) {
      reject(new Error('数据库未初始化'))
      return
    }
    const transaction = db.transaction([STORE_NAME], 'readwrite')
    const objectStore = transaction.objectStore(STORE_NAME)
    const request = objectStore.delete(sessionId)
    
    request.onsuccess = () => {
      resolve()
    }
    
    request.onerror = () => {
      console.error('删除历史记录失败')
      reject(request.error)
    }
  })
}

// 导出记录
const exportRecord = (record) => {
  const content = generateMarkdown(record)
  downloadMarkdown(content, record.title || '总结记录')
}

// 生成 Markdown 内容
const generateMarkdown = (record) => {
  const title = record.title || '无标题'
  const date = record.startTime ? new Date(record.startTime).toLocaleString() : ''
  const duration = formatDuration(record.duration)
  
  let md = `# ${title}\n\n`
  md += `**时间**: ${date}\n`
  md += `**时长**: ${duration}\n\n`
  
  if (record.summary && record.summary.content) {
    md += `## 总结\n\n${record.summary.content}\n\n`
  }
  
  if (record.summary && record.summary.keyPoints && record.summary.keyPoints.length > 0) {
    md += `## 关键点\n\n`
    record.summary.keyPoints.forEach((point, index) => {
      md += `${index + 1}. ${point}\n`
    })
    md += `\n`
  }
  
  return md
}

// 下载 Markdown 文件
const downloadMarkdown = (content, filename) => {
  const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${filename}_${new Date().toISOString().slice(0, 10)}.md`
  a.click()
  URL.revokeObjectURL(url)
}

// 事件处理
const handleExport = () => {
  if (props.currentSession) {
    const content = generateMarkdown({
      title: props.currentSession.title || '当前会话',
      startTime: props.currentSession.startTime || Date.now(),
      duration: props.currentSession.duration || 0,
      summary: props.currentSession.summary
    })
    downloadMarkdown(content, '当前会话总结')
    emit('export', { sessionId: props.currentSession.sessionId, format: 'md' })
  }
}

const handleCopy = async () => {
  if (displaySummary.value) {
    const text = `${displaySummary.value.summary || ''}\n\n关键点:\n${(displaySummary.value.keyPoints || []).map(p => `- ${p}`).join('\n')}`
    try {
      await navigator.clipboard.writeText(text)
      alert('已复制到剪贴板')
    } catch (err) {
      console.error('复制失败:', err)
    }
  }
}

const handleFavorite = () => {
  if (props.currentSession) {
    isCurrentFavorite.value = !isCurrentFavorite.value
    emit('favorite', { sessionId: props.currentSession.sessionId, isFavorite: isCurrentFavorite.value })
  }
}

const toggleFavorite = (record) => {
  record.isFavorite = !record.isFavorite
  saveHistoryRecord(record)
  emit('favorite', { sessionId: record.sessionId, isFavorite: record.isFavorite })
}

const deleteRecord = (record) => {
  if (confirm(`确定要删除记录"${record.title || '无标题'}"吗？`)) {
    deleteHistoryRecord(record.sessionId)
    emit('deleteHistory', { sessionId: record.sessionId })
  }
}

// 监听总结更新状态
watch(() => props.currentSession?.summary, (newSummary) => {
  if (newSummary) {
    isSummaryUpdating.value = false
  }
}, { immediate: true })

watch(() => props.currentSession?.status, (newStatus) => {
  if (newStatus === 'recording') {
    isSummaryUpdating.value = true
  }
}, { immediate: true })

// Provide/Inject - 与 RecordingPanel 共享状态
provide('summaryPanel', {
  sessionId: computed(() => props.currentSession?.sessionId),
  summary: computed(() => props.currentSession?.summary),
  transcripts: computed(() => props.currentSession?.transcripts || []),
  status: computed(() => props.currentSession?.status),
  isSummaryUpdating
})

// 生命周期
onMounted(async () => {
  await initDB()
  const records = await getAllHistoryRecords()
  historyCount.value = records.length
})

onUnmounted(() => {
  if (db) {
    db.close()
    db = null
  }
})

// 暴露方法给父组件
defineExpose({
  saveCurrentSession: async (sessionData) => {
    if (sessionData && sessionData.sessionId) {
      await saveHistoryRecord(sessionData)
      historyCount.value++
    }
  },
  getHistoryRecords: getAllHistoryRecords
})
</script>

<style scoped>
.summary-panel {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease-in-out;
}

/* 布局模式 */
.layout-sidebar {
  width: 360px;
  min-width: 360px;
  max-width: 400px;
  height: 100%;
}

.layout-panel {
  width: 100%;
  min-height: 400px;
}

.layout-embedded {
  width: 100%;
  min-height: 300px;
}

/* 收起状态 */
.collapsed .panel-content,
.collapsed .panel-footer {
  display: none;
}

.collapsed {
  width: auto;
  min-width: auto;
}

/* Header */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.idle {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

.status-badge.connecting {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.status-badge.recording {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.status-badge.paused {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.status-badge.ended {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collapse-btn {
  padding: 4px 8px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s ease-in-out;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
}

/* Tab Bar */
.tab-bar {
  display: flex;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.02);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.tab-item {
  padding: 12px 16px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-item:hover {
  color: rgba(255, 255, 255, 0.9);
}

.tab-item.active {
  color: #7c3aed;
  border-bottom-color: #7c3aed;
}

.tab-item .badge {
  padding: 2px 6px;
  background: rgba(124, 58, 237, 0.2);
  color: #7c3aed;
  border-radius: 10px;
  font-size: 12px;
}

/* Panel Content */
.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

/* Summary Card */
.summary-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.session-duration {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.05);
  padding: 2px 8px;
  border-radius: 10px;
}

.card-content {
  min-height: 80px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.6);
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top: 2px solid #7c3aed;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.key-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.key-point-tag {
  background: rgba(124, 58, 237, 0.15);
  color: #a78bfa;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.summary-text {
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 12px;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.summary-text:not(.expanded) {
  max-height: 80px;
  position: relative;
}

.summary-text:not(.expanded)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: rgba(255, 255, 255, 0.02);
}

.summary-text.expanded {
  max-height: 500px;
}

.expand-btn {
  background: transparent;
  border: none;
  color: #7c3aed;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 0;
}

.expand-btn:hover {
  text-decoration: underline;
}

.empty-state {
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  padding: 20px;
  font-style: italic;
}

.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.update-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* Transcript Reference */
.transcript-ref {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}

.transcript-toggle {
  width: 100%;
  padding: 12px 16px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: left;
}

.transcript-toggle:hover {
  background: rgba(255, 255, 255, 0.02);
}

.transcript-count {
  color: rgba(255, 255, 255, 0.4);
}

.toggle-icon {
  margin-left: auto;
  font-size: 10px;
}

.transcript-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 0 16px 12px;
}

.transcript-item {
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 12px;
  display: flex;
  gap: 8px;
}

.transcript-item:last-child {
  border-bottom: none;
}

.transcript-time {
  color: rgba(255, 255, 255, 0.4);
  flex-shrink: 0;
}

.transcript-text {
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.4;
}

/* History Records */
.history-records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-bar {
  padding: 0;
}

.search-input {
  width: 100%;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  outline: none;
  transition: border-color 0.15s ease;
}

.search-input:focus {
  border-color: #7c3aed;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.12);
}

.history-item.selected {
  border-color: #7c3aed;
  background: rgba(124, 58, 237, 0.1);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.history-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  padding: 4px 6px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: 12px;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.history-title {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 4px;
}

.history-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
}

.history-preview {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.4;
}

.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  text-align: center;
}

/* Footer */
.panel-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.02);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.footer-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.footer-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.footer-btn.active {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.3);
  color: #f59e0b;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .layout-sidebar {
    width: 100%;
    min-width: 100%;
    max-width: 100%;
  }
  
  .panel-header {
    padding: 12px;
  }
  
  .panel-content {
    padding: 12px;
  }
  
  .tab-item {
    padding: 10px 12px;
    font-size: 13px;
  }
  
  .summary-card {
    padding: 12px;
  }
  
  .panel-footer {
    gap: 12px;
    padding: 10px 12px;
  }
  
  .footer-btn {
    padding: 6px 12px;
    font-size: 12px;
  }
}

@media (min-width: 768px) and (max-width: 1023px) {
  .layout-sidebar {
    width: 320px;
    min-width: 320px;
    max-width: 320px;
  }
}
</style>
