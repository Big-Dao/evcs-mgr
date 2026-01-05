<template>
  <div class="station-map-container" v-loading="loading">
    <div id="station-map" ref="mapContainer" class="map-view"></div>
    <div v-if="error" class="map-error">
      <el-empty :description="error" />
    </div>
    <div v-if="!latitude || !longitude" class="map-empty">
      <el-empty description="暂无位置信息" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onBeforeUnmount, nextTick } from 'vue'

const props = defineProps<{
  latitude?: number | string
  longitude?: number | string
  stationName?: string
}>()

const mapContainer = ref<HTMLElement | null>(null)
const loading = ref(false)
const error = ref('')
let mapInstance: any = null

// Configuration
const mapConfig = {
  provider: import.meta.env.VITE_MAP_PROVIDER || 'amap', // 'amap' or 'baidu'
  amapKey: import.meta.env.VITE_AMAP_KEY || '',
  baiduKey: import.meta.env.VITE_BAIDU_KEY || ''
}

const loadScript = (src: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src^="${src.split('?')[0]}"]`)) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load script: ${src}`))
    document.head.appendChild(script)
  })
}

const initAmap = async () => {
  if (!mapConfig.amapKey) {
    error.value = '请配置高德地图 Key (VITE_AMAP_KEY)'
    return
  }
  try {
    if (!(window as any).AMap) {
        // 使用安全密钥 (如果需要) - 这里仅演示基本加载
        // (window as any)._AMapSecurityConfig = { securityJsCode: '...' }
        await loadScript(`https://webapi.amap.com/maps?v=2.0&key=${mapConfig.amapKey}`)
    }
    
    const AMap = (window as any).AMap
    const lng = Number(props.longitude)
    const lat = Number(props.latitude)
    
    if (isNaN(lng) || isNaN(lat)) {
        return
    }

    mapInstance = new AMap.Map(mapContainer.value, {
      zoom: 15,
      center: [lng, lat],
    })

    const marker = new AMap.Marker({
      position: [lng, lat],
      title: props.stationName
    })
    mapInstance.add(marker)

  } catch (e: any) {
    error.value = '地图加载失败: ' + e.message
    console.error(e)
  }
}

const initBaiduMap = async () => {
   if (!mapConfig.baiduKey) {
    error.value = '请配置百度地图 Key (VITE_BAIDU_KEY)'
    return
  }
  try {
    const callbackName = 'initBMapCallback_' + Date.now()
    if (!(window as any).BMap) {
        (window as any)[callbackName] = () => {
            renderBaiduMap()
            delete (window as any)[callbackName]
        }
        await loadScript(`https://api.map.baidu.com/api?v=3.0&ak=${mapConfig.baiduKey}&callback=${callbackName}`)
    } else {
        renderBaiduMap()
    }
  } catch (e: any) {
      error.value = '地图加载失败: ' + e.message
  }
}

const renderBaiduMap = () => {
    const BMap = (window as any).BMap
    const lng = Number(props.longitude)
    const lat = Number(props.latitude)
    
    if (isNaN(lng) || isNaN(lat)) {
        return
    }

    mapInstance = new BMap.Map(mapContainer.value)
    const point = new BMap.Point(lng, lat)
    mapInstance.centerAndZoom(point, 15)
    mapInstance.enableScrollWheelZoom(true)

    const marker = new BMap.Marker(point)
    mapInstance.addOverlay(marker)
    
    if (props.stationName) {
        const label = new BMap.Label(props.stationName, { offset: new BMap.Size(20, -10) })
        marker.setLabel(label)
    }
}

const initMap = async () => {
  if (!props.latitude || !props.longitude) {
      return
  }
  
  loading.value = true
  error.value = ''

  try {
    await nextTick()
    if (mapConfig.provider === 'baidu') {
      await initBaiduMap()
    } else {
      await initAmap()
    }
  } catch (e: any) {
      error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initMap()
})

watch(() => [props.latitude, props.longitude], () => {
    if (mapInstance) {
        if (mapInstance.destroy) mapInstance.destroy()
        mapInstance = null
        if (mapContainer.value) mapContainer.value.innerHTML = ''
    }
    initMap()
})

onBeforeUnmount(() => {
    if (mapInstance && mapInstance.destroy) {
        mapInstance.destroy()
    }
})
</script>

<style scoped>
.station-map-container {
  width: 100%;
  height: 400px;
  position: relative;
  background-color: #f5f7fa;
  border-radius: 4px;
  overflow: hidden;
}
.map-view {
  width: 100%;
  height: 100%;
}
.map-error, .map-empty {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fff;
  z-index: 10;
}
</style>
