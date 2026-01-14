<template>
  <view class="page-container">
    <!-- 顶部搜索栏 -->
    <view class="header">
      <view class="location" @click="handleLocationClick">
        <text class="location-icon">📍</text>
        <text class="location-text ellipsis">{{ currentCity }}</text>
        <text class="location-arrow">▼</text>
      </view>
      <view class="search-bar" @click="goToSearch">
        <text class="search-icon">🔍</text>
        <text class="search-placeholder">搜索充电站</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="shortcuts card">
      <view class="shortcut-item" @click="goToScan">
        <view class="shortcut-icon scan">📷</view>
        <text class="shortcut-text">扫码充电</text>
      </view>
      <view class="shortcut-item" @click="goToMap">
        <view class="shortcut-icon map">🗺️</view>
        <text class="shortcut-text">附近站点</text>
      </view>
      <view class="shortcut-item" @click="goToOrders">
        <view class="shortcut-icon order">📋</view>
        <text class="shortcut-text">我的订单</text>
      </view>
      <view class="shortcut-item" @click="goToWallet">
        <view class="shortcut-icon wallet">💰</view>
        <text class="shortcut-text">我的钱包</text>
      </view>
    </view>

    <!-- 充电中提示 -->
    <view v-if="isCharging" class="charging-banner card" @click="goToChargingSession">
      <view class="charging-info">
        <view class="charging-status">
          <text class="charging-dot"></text>
          <text class="charging-label">充电中</text>
        </view>
        <view class="charging-data">
          <text class="data-value">{{ formatEnergy(currentEnergy) }}</text>
          <text class="data-label">已充电量</text>
        </view>
        <view class="charging-data">
          <text class="data-value">{{ formatDuration(currentDuration) }}</text>
          <text class="data-label">充电时长</text>
        </view>
      </view>
      <view class="charging-action">
        <text>查看详情 ›</text>
      </view>
    </view>

    <!-- 附近充电站 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">附近充电站</text>
        <text class="section-more" @click="goToStationList">更多 ›</text>
      </view>
      
      <view v-if="loading" class="loading-wrapper">
        <text class="loading-text">加载中...</text>
      </view>
      
      <view v-else-if="nearbyStations.length === 0" class="empty-state">
        <text class="empty-text">暂无附近充电站</text>
      </view>
      
      <view v-else class="station-list">
        <view 
          v-for="station in nearbyStations" 
          :key="station.id" 
          class="station-card card"
          @click="goToStationDetail(station.id)"
        >
          <view class="station-info">
            <view class="station-name ellipsis">{{ station.name }}</view>
            <view class="station-address ellipsis">{{ station.address }}</view>
            <view class="station-meta">
              <text class="station-distance">{{ formatDistance(station.distance || 0) }}</text>
              <text class="station-divider">|</text>
              <text class="station-available text-success">空闲 {{ station.availableCount }}/{{ station.chargerCount }}</text>
            </view>
          </view>
          <view class="station-price">
            <text class="price-value">¥{{ station.minPrice?.toFixed(2) }}</text>
            <text class="price-unit">/度</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { useUserStore, useChargingStore, useLocationStore } from '@/stores'
import { getNearbyStations } from '@/api/station'
import { formatDistance, formatEnergy, formatDuration } from '@/utils/format'
import type { Station } from '@/types'

const userStore = useUserStore()
const chargingStore = useChargingStore()
const locationStore = useLocationStore()

const loading = ref(false)
const nearbyStations = ref<Station[]>([])
const currentCity = ref('定位中...')

const isCharging = computed(() => chargingStore.isCharging)
const currentEnergy = computed(() => chargingStore.currentEnergy)
const currentDuration = computed(() => chargingStore.currentDuration)

onMounted(async () => {
  await initData()
})

onShow(() => {
  // 检查是否有进行中的充电
  if (userStore.isLoggedIn) {
    chargingStore.checkActiveCharging()
  }
})

onPullDownRefresh(async () => {
  await initData()
  uni.stopPullDownRefresh()
})

async function initData() {
  // 获取位置
  const location = await locationStore.updateLocation()
  if (location) {
    currentCity.value = '当前位置'
    await loadNearbyStations(location.latitude, location.longitude)
  } else {
    currentCity.value = '定位失败'
  }
}

async function loadNearbyStations(latitude: number, longitude: number) {
  loading.value = true
  try {
    const res = await getNearbyStations(latitude, longitude)
    nearbyStations.value = res.data?.slice(0, 5) || []
  } catch (error) {
    console.error('Failed to load nearby stations:', error)
  } finally {
    loading.value = false
  }
}

function handleLocationClick() {
  // 重新定位
  initData()
}

function goToSearch() {
  uni.navigateTo({ url: '/pages/station/list?focus=true' })
}

function goToScan() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/charging/scan' })
}

function goToMap() {
  uni.navigateTo({ url: '/pages/station/map' })
}

function goToOrders() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.switchTab({ url: '/pages/order/list' })
}

function goToWallet() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/user/wallet' })
}

function goToStationList() {
  uni.switchTab({ url: '/pages/station/list' })
}

function goToStationDetail(stationId: number) {
  uni.navigateTo({ url: `/pages/station/detail?id=${stationId}` })
}

function goToChargingSession() {
  uni.navigateTo({ url: '/pages/charging/session' })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.header {
  display: flex;
  align-items: center;
  padding: $spacing-lg;
  background-color: $bg-color-white;
  gap: $spacing-md;
}

.location {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  max-width: 200rpx;
  
  .location-icon {
    font-size: $font-size-lg;
    margin-right: $spacing-xs;
  }
  
  .location-text {
    font-size: $font-size-md;
    color: $text-color;
    max-width: 120rpx;
  }
  
  .location-arrow {
    font-size: $font-size-xs;
    color: $text-color-tertiary;
    margin-left: $spacing-xs;
  }
}

.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  background-color: $fill-color-light;
  border-radius: $radius-full;
  padding: 0 $spacing-lg;
  
  .search-icon {
    font-size: $font-size-lg;
    margin-right: $spacing-sm;
  }
  
  .search-placeholder {
    color: $text-color-tertiary;
    font-size: $font-size-md;
  }
}

.shortcuts {
  display: flex;
  justify-content: space-around;
  margin: $spacing-lg;
  padding: $spacing-xl $spacing-lg;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .shortcut-icon {
    width: 96rpx;
    height: 96rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-lg;
    font-size: 48rpx;
    margin-bottom: $spacing-sm;
    
    &.scan { background-color: rgba($primary-color, 0.1); }
    &.map { background-color: rgba($info-color, 0.1); }
    &.order { background-color: rgba($warning-color, 0.1); }
    &.wallet { background-color: rgba($error-color, 0.1); }
  }
  
  .shortcut-text {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}

.charging-banner {
  margin: 0 $spacing-lg $spacing-lg;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, $primary-color 0%, $primary-color-dark 100%);
  color: #FFFFFF;
}

.charging-info {
  display: flex;
  align-items: center;
  gap: $spacing-xl;
}

.charging-status {
  display: flex;
  align-items: center;
  
  .charging-dot {
    width: 16rpx;
    height: 16rpx;
    background-color: #FFFFFF;
    border-radius: 50%;
    margin-right: $spacing-sm;
    animation: pulse 1.5s infinite;
  }
  
  .charging-label {
    font-size: $font-size-md;
    font-weight: 500;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.charging-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .data-value {
    font-size: $font-size-lg;
    font-weight: 600;
  }
  
  .data-label {
    font-size: $font-size-xs;
    opacity: 0.8;
  }
}

.charging-action {
  font-size: $font-size-sm;
  opacity: 0.9;
}

.section {
  margin: 0 $spacing-lg $spacing-lg;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  
  .section-title {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $text-color;
  }
  
  .section-more {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}

.station-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.station-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.station-price {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  margin-left: $spacing-md;
  
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
</style>
