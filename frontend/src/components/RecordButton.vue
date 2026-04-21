<template>
  <button 
    class="record-button" 
    :class="[status, { disabled }]"
    :disabled="disabled"
    @click="$emit('click')"
  >
    <div class="button-content">
      <div class="icon" v-if="status === 'idle'">🎤</div>
      <div class="icon loading" v-else-if="status === 'connecting'"></div>
      <div class="icon" v-else-if="status === 'recording'">⏸️</div>
      <div class="icon" v-else-if="status === 'paused'">▶️</div>
      <div class="icon" v-else-if="status === 'ended'">🔄</div>
      <div class="text">{{ buttonText }}</div>
    </div>
  </button>
</template>

<script setup>
import { computed } from 'vue'

// Props
const props = defineProps({
  status: {
    type: String,
    default: 'idle'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['click'])

// 按钮文本
const buttonText = computed(() => {
  switch (props.status) {
    case 'idle':
      return '开始录音'
    case 'connecting':
      return '连接中...'
    case 'recording':
      return '暂停'
    case 'paused':
      return '继续'
    case 'ended':
      return '开始新录音'
    default:
      return '开始录音'
  }
})
</script>

<style scoped>
.record-button {
  position: relative;
  width: 100px;
  height: 100px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.record-button.idle {
  background: #3498db;
  box-shadow: 0 4px 15px rgba(52, 152, 219, 0.4);
}

.record-button.idle:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(52, 152, 219, 0.6);
}

.record-button.connecting {
  background: #f39c12;
  box-shadow: 0 4px 15px rgba(243, 156, 18, 0.4);
  cursor: not-allowed;
}

.record-button.recording {
  background: #e74c3c;
  box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);
  animation: pulse 2s infinite;
}

.record-button.recording:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(231, 76, 60, 0.6);
}

.record-button.paused {
  background: #27ae60;
  box-shadow: 0 4px 15px rgba(39, 174, 96, 0.4);
}

.record-button.paused:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(39, 174, 96, 0.6);
}

.record-button.ended {
  background: #95a5a6;
  box-shadow: 0 4px 15px rgba(149, 165, 166, 0.4);
}

.record-button.ended:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(149, 165, 166, 0.6);
}

.record-button.disabled {
  background: #bdc3c7;
  box-shadow: 0 4px 15px rgba(189, 195, 199, 0.4);
  cursor: not-allowed;
}

.button-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 500;
}

.icon {
  font-size: 32px;
  margin-bottom: 4px;
}

.icon.loading {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top: 3px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}

.text {
  font-size: 14px;
  text-align: center;
}

@keyframes pulse {
  0% {
    box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);
  }
  50% {
    box-shadow: 0 6px 25px rgba(231, 76, 60, 0.8);
  }
  100% {
    box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .record-button {
    width: 80px;
    height: 80px;
  }
  
  .icon {
    font-size: 24px;
  }
  
  .text {
    font-size: 12px;
  }
}

@media (min-width: 768px) and (max-width: 1024px) {
  .record-button {
    width: 90px;
    height: 90px;
  }
  
  .icon {
    font-size: 28px;
  }
  
  .text {
    font-size: 13px;
  }
}
</style>