import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Location } from '@/types'
import { getCurrentLocation } from '@/utils/location'

export const useLocationStore = defineStore('location', () => {
  // State
  const currentLocation = ref<Location | null>(null)
  const locationError = ref<string>('')
  const isLoading = ref(false)
  
  // Actions
  
  /**
   * 获取当前位置
   */
  async function updateLocation() {
    if (isLoading.value) return currentLocation.value
    
    isLoading.value = true
    locationError.value = ''
    
    try {
      const location = await getCurrentLocation()
      currentLocation.value = location
      return location
    } catch (error: unknown) {
      const err = error as { errMsg?: string }
      console.error('Failed to get location:', error)
      locationError.value = err?.errMsg || '获取位置失败'
      return null
    } finally {
      isLoading.value = false
    }
  }
  
  /**
   * 设置位置
   */
  function setLocation(location: Location) {
    currentLocation.value = location
  }
  
  /**
   * 清空位置
   */
  function clearLocation() {
    currentLocation.value = null
    locationError.value = ''
  }
  
  return {
    // State
    currentLocation,
    locationError,
    isLoading,
    // Actions
    updateLocation,
    setLocation,
    clearLocation
  }
})
