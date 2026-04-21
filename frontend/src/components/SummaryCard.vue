<template>
  <div class="summary-card">
    <h3 class="title">实时总结</h3>
    <div class="summary-content">
      <div v-if="isUpdating" class="loading-state">
        <div class="loading-spinner"></div>
        <span>AI 正在生成总结...</span>
      </div>
      <div v-else-if="summary.summary" class="summary-text">
        <div class="key-points">
          <span 
            v-for="(point, index) in summary.keyPoints" 
            :key="index"
            class="key-point"
          >
            📌 {{ point }}
          </span>
        </div>
        <div class="summary-details" :class="{ expanded: isExpanded }">
          {{ summary.summary }}
        </div>
        <button class="expand-btn" @click="isExpanded = !isExpanded">
          {{ isExpanded ? '收起' : '展开完整总结' }} {{ isExpanded ? '▲' : '▼' }}
        </button>
      </div>
      <div v-else class="empty-state">
        开始录音后 AI 将生成实时总结
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// Props
const props = defineProps({
  summary: {
    type: Object,
    default: () => ({ summary: '', keyPoints: [] })
  },
  isUpdating: {
    type: Boolean,
    default: false
  }
})

// 状态
const isExpanded = ref(false)
</script>

<style scoped>
.summary-card {
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

.summary-content {
  min-height: 80px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #666;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top: 2px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 10px;
}

.key-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.key-point {
  background: #e3f2fd;
  color: #1976d2;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.summary-details {
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  margin-bottom: 12px;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.summary-details:not(.expanded) {
  max-height: 80px;
  position: relative;
}

.summary-details:not(.expanded)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(transparent, white);
}

.summary-details.expanded {
  max-height: 500px;
}

.expand-btn {
  background: none;
  border: none;
  color: #3498db;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.expand-btn:hover {
  text-decoration: underline;
}

.empty-state {
  text-align: center;
  color: #999;
  padding: 20px;
  font-style: italic;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .summary-card {
    padding: 12px;
  }
  
  .title {
    font-size: 14px;
  }
  
  .key-point {
    font-size: 11px;
    padding: 3px 10px;
  }
  
  .summary-details {
    font-size: 13px;
  }
}
</style>