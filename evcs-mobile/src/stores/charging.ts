import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Order, ChargingSession } from '@/types'
import { getActiveOrder, getChargingSession, stopCharging as stopChargingApi } from '@/api/order'

export const useChargingStore = defineStore('charging', () => {
  // State
  const activeOrder = ref<Order | null>(null)
  const chargingSession = ref<ChargingSession | null>(null)
  const isPolling = ref(false)
  
  let pollingTimer: ReturnType<typeof setInterval> | null = null
  
  // Getters
  const isCharging = computed(() => !!activeOrder.value && activeOrder.value.status === 'CHARGING')
  const currentOrderId = computed(() => activeOrder.value?.id)
  const currentEnergy = computed(() => chargingSession.value?.energy || 0)
  const currentDuration = computed(() => chargingSession.value?.duration || 0)
  const currentPower = computed(() => chargingSession.value?.power || 0)
  const currentAmount = computed(() => chargingSession.value?.currentAmount || 0)
  const currentSoc = computed(() => chargingSession.value?.soc || 0)
  
  // Actions
  
  /**
   * 检查是否有进行中的充电
   */
  async function checkActiveCharging() {
    try {
      const res = await getActiveOrder()
      if (res.data) {
        activeOrder.value = res.data
        if (res.data.status === 'CHARGING') {
          startPolling()
        }
      } else {
        activeOrder.value = null
        chargingSession.value = null
      }
    } catch (error) {
      console.error('Failed to check active charging:', error)
    }
  }
  
  /**
   * 设置当前订单
   */
  function setActiveOrder(order: Order) {
    activeOrder.value = order
    if (order.status === 'CHARGING') {
      startPolling()
    }
  }
  
  /**
   * 开始轮询充电状态
   */
  function startPolling() {
    if (isPolling.value || !activeOrder.value) return
    
    isPolling.value = true
    
    // 立即获取一次
    fetchChargingSession()
    
    // 每 5 秒轮询一次
    pollingTimer = setInterval(() => {
      fetchChargingSession()
    }, 5000)
  }
  
  /**
   * 停止轮询
   */
  function stopPolling() {
    isPolling.value = false
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }
  
  /**
   * 获取充电会话数据
   */
  async function fetchChargingSession() {
    if (!activeOrder.value) return
    
    try {
      const res = await getChargingSession(activeOrder.value.id)
      if (res.data) {
        chargingSession.value = res.data
        
        // 检查充电是否已结束
        if (res.data.status !== 'CHARGING') {
          stopPolling()
          activeOrder.value = null
        }
      }
    } catch (error) {
      console.error('Failed to fetch charging session:', error)
    }
  }
  
  /**
   * 停止充电
   */
  async function stopCharging() {
    if (!activeOrder.value) return
    
    try {
      const res = await stopChargingApi(activeOrder.value.id)
      if (res.data) {
        stopPolling()
        const completedOrder = res.data
        activeOrder.value = null
        chargingSession.value = null
        return completedOrder
      }
    } catch (error) {
      console.error('Failed to stop charging:', error)
      throw error
    }
  }
  
  /**
   * 清空状态
   */
  function clearState() {
    stopPolling()
    activeOrder.value = null
    chargingSession.value = null
  }
  
  return {
    // State
    activeOrder,
    chargingSession,
    isPolling,
    // Getters
    isCharging,
    currentOrderId,
    currentEnergy,
    currentDuration,
    currentPower,
    currentAmount,
    currentSoc,
    // Actions
    checkActiveCharging,
    setActiveOrder,
    startPolling,
    stopPolling,
    fetchChargingSession,
    stopCharging,
    clearState
  }
})
