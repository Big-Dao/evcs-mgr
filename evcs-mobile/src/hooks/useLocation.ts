import { ref, onMounted, onUnmounted } from 'vue'

export interface Location {
  latitude: number
  longitude: number
  accuracy?: number
  address?: string
}

export function useLocation() {
  const location = ref<Location | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  
  let watchId: number | null = null
  
  async function getLocation(): Promise<Location | null> {
    loading.value = true
    error.value = null
    
    try {
      const result = await new Promise<Location>((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02',
          success: (res) => {
            resolve({
              latitude: res.latitude,
              longitude: res.longitude,
              accuracy: res.accuracy
            })
          },
          fail: (err) => {
            reject(err)
          }
        })
      })
      
      location.value = result
      return result
    } catch (e) {
      console.error('Get location error:', e)
      error.value = '获取位置失败，请检查定位权限'
      return null
    } finally {
      loading.value = false
    }
  }
  
  function startWatch() {
    // #ifdef MP-WEIXIN || MP-ALIPAY
    uni.startLocationUpdate({
      success: () => {
        uni.onLocationChange((res) => {
          location.value = {
            latitude: res.latitude,
            longitude: res.longitude,
            accuracy: res.accuracy
          }
        })
      }
    })
    // #endif
  }
  
  function stopWatch() {
    // #ifdef MP-WEIXIN || MP-ALIPAY
    uni.stopLocationUpdate({})
    uni.offLocationChange(() => {})
    // #endif
    
    if (watchId !== null) {
      clearInterval(watchId)
      watchId = null
    }
  }
  
  function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371000 // 地球半径（米）
    const dLat = toRad(lat2 - lat1)
    const dLon = toRad(lon2 - lon1)
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2)
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return R * c
  }
  
  function toRad(deg: number): number {
    return deg * (Math.PI / 180)
  }
  
  onUnmounted(() => {
    stopWatch()
  })
  
  return {
    location,
    loading,
    error,
    getLocation,
    startWatch,
    stopWatch,
    calculateDistance
  }
}
