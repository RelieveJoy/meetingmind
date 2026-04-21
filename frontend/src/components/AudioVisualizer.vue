<template>
  <div class="audio-visualizer">
    <div class="visualizer-container">
      <canvas ref="canvasRef" class="waveform"></canvas>
      <div class="recording-indicator" v-if="isActive"></div>
    </div>
    <div class="time-display">
      {{ formatTime(duration) }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'

// Props
const props = defineProps({
  audioData: {
    type: Uint8Array,
    default: null
  },
  duration: {
    type: Number,
    default: 0
  },
  isActive: {
    type: Boolean,
    default: false
  },
  maxDuration: {
    type: Number,
    default: null
  }
})

// 引用
const canvasRef = ref(null)

// 状态
let animationId = null
let canvas = null
let ctx = null

// 格式化时间
const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 绘制波形
const drawWaveform = () => {
  if (!canvas || !ctx) return
  
  const width = canvas.width
  const height = canvas.height
  
  // 清空画布
  ctx.fillStyle = '#1a1a2e'
  ctx.fillRect(0, 0, width, height)
  
  // 绘制波形
  if (props.audioData) {
    const dataArray = props.audioData
    const bufferLength = dataArray.length
    
    ctx.lineWidth = 2
    ctx.strokeStyle = '#00d4ff'
    ctx.beginPath()
    
    const sliceWidth = (width * 1.0) / bufferLength
    let x = 0
    
    for (let i = 0; i < bufferLength; i++) {
      const v = dataArray[i] / 128.0
      const y = v * (height / 2)
      
      if (i === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
      
      x += sliceWidth
    }
    
    ctx.lineTo(width, height / 2)
    ctx.stroke()
  } else {
    // 静态波形（未录音时）
    ctx.lineWidth = 2
    ctx.strokeStyle = '#444'
    ctx.beginPath()
    
    const centerY = height / 2
    const segments = 50
    const segmentWidth = width / segments
    
    for (let i = 0; i <= segments; i++) {
      const x = i * segmentWidth
      const y = centerY + (Math.sin(i * 0.1) * 20)
      
      if (i === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    }
    
    ctx.stroke()
  }
  
  // 绘制进度条
  if (props.maxDuration) {
    const progress = Math.min((props.duration / props.maxDuration) * width, width)
    ctx.fillStyle = 'rgba(0, 212, 255, 0.3)'
    ctx.fillRect(0, 0, progress, height)
  }
  
  animationId = requestAnimationFrame(drawWaveform)
}

// 监听音频数据变化
watch(() => props.audioData, () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  drawWaveform()
})

// 生命周期
onMounted(() => {
  canvas = canvasRef.value
  if (canvas) {
    // 设置 canvas 尺寸
    canvas.width = canvas.offsetWidth
    canvas.height = canvas.offsetHeight
    ctx = canvas.getContext('2d')
    drawWaveform()
  }
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped>
.audio-visualizer {
  width: 100%;
  background: #1a1a2e;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.visualizer-container {
  position: relative;
  width: 100%;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
}

.waveform {
  width: 100%;
  height: 100%;
  display: block;
}

.recording-indicator {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 12px;
  height: 12px;
  background: #ff4757;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

.time-display {
  text-align: center;
  margin-top: 10px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  background: rgba(0, 0, 0, 0.3);
  padding: 4px 12px;
  border-radius: 12px;
  display: inline-block;
  margin-left: 50%;
  transform: translateX(-50%);
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
  100% {
    transform: scale(0.8);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .visualizer-container {
    height: 80px;
  }
  
  .audio-visualizer {
    padding: 12px;
  }
}

@media (min-width: 768px) and (max-width: 1024px) {
  .visualizer-container {
    height: 100px;
  }
}
</style>