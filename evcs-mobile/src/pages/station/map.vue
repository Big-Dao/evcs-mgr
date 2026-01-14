<template>
  <view class="page-container">
    <!-- 地图 -->
    <map
      id="stationMap"
      class="station-map"
      :latitude="mapCenter.latitude"
      :longitude="mapCenter.longitude"
      :markers="markers"
      :scale="14"
      show-location
      @markertap="onMarkerTap"
      @regionchange="onRegionChange"
    />
    
    <!-- 顶部搜索栏 -->
    <view class="search-overlay safe-area-top">
      <view class="search-bar" @click="goToSearch">
        <text class="search-icon">🔍</text>
        <text class="search-placeholder">搜索充电站</text>
      </view>
    </view>
    
    <!-- 控制按钮 -->
    <view class="map-controls">
      <view class="control-btn" @click="relocate">
        <text>📍</text>
      </view>
      <view class="control-btn" @click="refreshStations">
        <text>🔄</text>
      </view>
    </view>
    
    <!-- 底部站点信息卡片 -->
    <view class="bottom-sheet" v-if="selectedStation">
      <view class="station-card" @click="goToDetail">
        <view class="station-info">
          <view class="station-name ellipsis">{{ selectedStation.name }}</view>
          <view class="station-address ellipsis">{{ selectedStation.address }}</view>
          <view class="station-meta">
            <text class="station-distance">{{ formatDistance(selectedStation.distance || 0) }}</text>
            <text class="station-divider">|</text>
            <text class="station-available">
              空闲 <text class="text-success">{{ selectedStation.availableCount }}</text>/{{ selectedStation.chargerCount }}
            </text>
          </view>
        </view>
        <view class="station-action">
          <view class="station-price">
            <text class="price-value">¥{{ selectedStation.minPrice?.toFixed(2) }}</text>
            <text class="price-unit">/度</text>
          </view>
          <view class="btn btn-primary btn-sm" @click.stop="goToNavigation">
            导航
          </view>
        </view>
      </view>
    </view>
    
    <!-- 站点列表（底部抽屉） -->
    <view class="station-list-drawer" :class="{ expanded: showList }">
      <view class="drawer-handle" @click="toggleList">
        <view class="handle-bar"></view>
        <text class="drawer-title">附近 {{ stations.length }} 个充电站</text>
      </view>
      
      <scroll-view class="drawer-content" scroll-y v-if="showList">
        <view 
          v-for="station in stations" 
          :key="station.id"
          class="list-item"
          @click="selectStation(station)"
        >
          <view class="item-info">
            <view class="item-name ellipsis">{{ station.name }}</view>
            <view class="item-meta">
              <text>{{ formatDistance(station.distance || 0) }}</text>
              <text class="text-success ml-sm">空闲{{ station.availableCount }}</text>
            </view>
          </view>
          <view class="item-price">
            <text class="price-value">¥{{ station.minPrice?.toFixed(2) }}</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useLocationStore } from '@/stores'
import { getNearbyStations } from '@/api/station'
import { formatDistance } from '@/utils/format'
import type { Station } from '@/types'

const locationStore = useLocationStore()

const mapCenter = ref({
  latitude: 39.9042,
  longitude: 116.4074
})

const stations = ref<Station[]>([])
const selectedStation = ref<Station | null>(null)
const showList = ref(false)
const loading = ref(false)

const markers = computed(() => {
  return stations.value.map(station => ({
    id: station.id,
    latitude: station.latitude,
    longitude: station.longitude,
    title: station.name,
    iconPath: station.id === selectedStation.value?.id 
      ? '/static/images/marker-active.png'
      : '/static/images/marker.png',
    width: 32,
    height: 40,
    callout: {
      content: `${station.name}\n空闲${station.availableCount}/${station.chargerCount}`,
      display: 'BYCLICK',
      padding: 8,
      borderRadius: 4,
      fontSize: 12
    }
  }))
})

onMounted(async () => {
  await initLocation()
})

async function initLocation() {
  const location = await locationStore.updateLocation()
  if (location) {
    mapCenter.value = {
      latitude: location.latitude,
      longitude: location.longitude
    }
    await loadNearbyStations()
  }
}

async function loadNearbyStations() {
  if (loading.value) return
  
  loading.value = true
  try {
    const res = await getNearbyStations(
      mapCenter.value.latitude,
      mapCenter.value.longitude,
      10000
    )
    stations.value = res.data || []
    
    // 默认选中第一个
    if (stations.value.length > 0 && !selectedStation.value) {
      selectedStation.value = stations.value[0]
    }
  } catch (error) {
    console.error('Failed to load stations:', error)
  } finally {
    loading.value = false
  }
}

function onMarkerTap(e: { markerId: number }) {
  const station = stations.value.find(s => s.id === e.markerId)
  if (station) {
    selectedStation.value = station
    showList.value = false
  }
}

function onRegionChange(e: { type: string; causedBy: string }) {
  if (e.type === 'end' && e.causedBy === 'drag') {
    // 拖动结束后可以重新加载附近站点
  }
}

async function relocate() {
  const location = await locationStore.updateLocation()
  if (location) {
    mapCenter.value = {
      latitude: location.latitude,
      longitude: location.longitude
    }
    
    // 移动地图到当前位置
    const mapContext = uni.createMapContext('stationMap')
    mapContext.moveToLocation({
      latitude: location.latitude,
      longitude: location.longitude
    })
    
    await loadNearbyStations()
  }
}

function refreshStations() {
  loadNearbyStations()
}

function selectStation(station: Station) {
  selectedStation.value = station
  showList.value = false
  
  // 移动地图到选中的站点
  const mapContext = uni.createMapContext('stationMap')
  mapContext.moveToLocation({
    latitude: station.latitude,
    longitude: station.longitude
  })
}

function toggleList() {
  showList.value = !showList.value
}

function goToSearch() {
  uni.navigateTo({ url: '/pages/station/list?focus=true' })
}

function goToDetail() {
  if (!selectedStation.value) return
  uni.navigateTo({ url: `/pages/station/detail?id=${selectedStation.value.id}` })
}

function goToNavigation() {
  if (!selectedStation.value) return
  
  uni.openLocation({
    latitude: selectedStation.value.latitude,
    longitude: selectedStation.value.longitude,
    name: selectedStation.value.name,
    address: selectedStation.value.address,
    scale: 18
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.page-container {
  position: relative;
  height: 100vh;
  overflow: hidden;
}

.station-map {
  width: 100%;
  height: 100%;
}

.search-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: $spacing-lg;
  z-index: 100;
}

.search-bar {
  display: flex;
  align-items: center;
  height: 80rpx;
  background-color: $bg-color-white;
  border-radius: $radius-full;
  padding: 0 $spacing-lg;
  box-shadow: $shadow-md;
  
  .search-icon {
    font-size: $font-size-lg;
    margin-right: $spacing-sm;
  }
  
  .search-placeholder {
    color: $text-color-tertiary;
    font-size: $font-size-md;
  }
}

.map-controls {
  position: absolute;
  right: $spacing-lg;
  bottom: 400rpx;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  z-index: 100;
}

.control-btn {
  width: 80rpx;
  height: 80rpx;
  background-color: $bg-color-white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-md;
  font-size: $font-size-xl;
}

.bottom-sheet {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-lg;
  padding-bottom: calc(#{$spacing-lg} + constant(safe-area-inset-bottom));
  padding-bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));
  z-index: 100;
}

.station-card {
  display: flex;
  align-items: center;
  background-color: $bg-color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-lg;
}

.station-info {
  flex: 1;
  min-width: 0;
  
  .station-name {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
    margin-bottom: $spacing-xs;
  }
  
  .station-address {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
    margin-bottom: $spacing-sm;
  }
  
  .station-meta {
    display: flex;
    align-items: center;
    font-size: $font-size-sm;
    
    .station-distance {
      color: $text-color-secondary;
    }
    
    .station-divider {
      margin: 0 $spacing-sm;
      color: $border-color;
    }
  }
}

.station-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: $spacing-sm;
  margin-left: $spacing-md;
  
  .station-price {
    display: flex;
    align-items: baseline;
    
    .price-value {
      font-size: $font-size-xl;
      font-weight: 600;
      color: $primary-color;
    }
    
    .price-unit {
      font-size: $font-size-xs;
      color: $text-color-tertiary;
    }
  }
  
  .btn {
    width: 120rpx;
  }
}

.station-list-drawer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: $bg-color-white;
  border-radius: $radius-xl $radius-xl 0 0;
  box-shadow: $shadow-lg;
  z-index: 101;
  max-height: 160rpx;
  transition: max-height 0.3s ease;
  
  &.expanded {
    max-height: 60vh;
  }
}

.drawer-handle {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-md $spacing-lg;
  
  .handle-bar {
    width: 60rpx;
    height: 8rpx;
    background-color: $border-color;
    border-radius: $radius-full;
    margin-bottom: $spacing-sm;
  }
  
  .drawer-title {
    font-size: $font-size-md;
    color: $text-color-secondary;
  }
}

.drawer-content {
  max-height: calc(60vh - 100rpx);
  padding: 0 $spacing-lg;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md 0;
  border-bottom: 1px solid $border-color-light;
  
  &:last-child {
    border-bottom: none;
  }
}

.item-info {
  flex: 1;
  min-width: 0;
  
  .item-name {
    font-size: $font-size-md;
    color: $text-color;
    margin-bottom: $spacing-xs;
  }
  
  .item-meta {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}

.item-price {
  .price-value {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $primary-color;
  }
}
</style>
