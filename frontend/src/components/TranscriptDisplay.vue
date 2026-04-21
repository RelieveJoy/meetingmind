<template>
  <div class="transcript-display">
    <h3 class="title">实时转写</h3>
    <div class="transcript-list" ref="transcriptListRef">
      <div 
        v-for="item in transcripts" 
        :key="item.id"
        class="transcript-item"
      >
        <div class="transcript-header">
          <span class="time-range">
            [{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}]
          </span>
          <span class="confidence">
            置信度: {{ Math.round(item.confidence * 100) }}%
          </span>
        </div>
        <div class="transcript-content">
          <div class="original-text">
            {{ item.text }}
          </div>
          <div v-if="showOptimization && item.isOptimized" class="optimized-text">
            <span class="label">↓ AI 优化</span>
            {{ item.optimizedText }}
          </div>
        </div>
      </div>
      <div v-if="transcripts.length === 0" class="empty-state">
        开始录音后将显示转写内容
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'

// Props
const props = defineProps({
  transcripts: {
    type: Array,
    default: () => []
  },
  showOptimization: {
    type: Boolean,
    default: true
  }
})

// 引用
const transcriptListRef = ref(null)

// 格式化时间
const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 监听转写列表变化，自动滚动到底部
watch(() => props.transcripts.length, async () => {
  await nextTick()
  if (transcriptListRef.value) {
    transcriptListRef.value.scrollTop = transcriptListRef.value.scrollHeight
  }
})
</script>

<style scoped>
.transcript-display {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 8px;
}

.transcript-list {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

.transcript-item {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.transcript-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
}

.time-range {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 12px;
}

.confidence {
  font-weight: 500;
}

.transcript-content {
  font-size: 14px;
  line-height: 1.5;
}

.original-text {
  color: #333;
  margin-bottom: 8px;
}

.optimized-text {
  color: #27ae60;
  background: #f8fff8;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #27ae60;
}

.optimized-text .label {
  font-size: 12px;
  font-weight: 500;
  margin-right: 8px;
  opacity: 0.7;
}

.empty-state {
  text-align: center;
  color: #999;
  padding: 40px 20px;
  font-style: italic;
}

/* 滚动条样式 */
.transcript-list::-webkit-scrollbar {
  width: 6px;
}

.transcript-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.transcript-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.transcript-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

@media (max-width: 768px) {
  .transcript-list {
    max-height: 200px;
  }
  
  .transcript-display {
    padding: 12px;
  }
  
  .title {
    font-size: 14px;
  }
  
  .transcript-content {
    font-size: 13px;
  }
}

@media (min-width: 768px) and (max-width: 1024px) {
  .transcript-list {
    max-height: 300px;
  }
}
</style>