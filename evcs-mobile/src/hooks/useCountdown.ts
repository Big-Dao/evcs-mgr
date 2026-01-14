import { ref, onMounted, onUnmounted } from 'vue'

export interface CountdownOptions {
  duration: number // 秒
  onComplete?: () => void
}

export function useCountdown(options: CountdownOptions) {
  const { duration, onComplete } = options
  
  const remaining = ref(0)
  const isRunning = ref(false)
  
  let timer: ReturnType<typeof setInterval> | null = null
  
  function start() {
    if (isRunning.value) return
    
    remaining.value = duration
    isRunning.value = true
    
    timer = setInterval(() => {
      remaining.value--
      
      if (remaining.value <= 0) {
        stop()
        onComplete?.()
      }
    }, 1000)
  }
  
  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isRunning.value = false
    remaining.value = 0
  }
  
  function reset() {
    stop()
    remaining.value = duration
  }
  
  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
    }
  })
  
  return {
    remaining,
    isRunning,
    start,
    stop,
    reset
  }
}
