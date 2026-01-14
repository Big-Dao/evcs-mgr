<template>
  <view class="page-container">
    <view v-if="loading" class="loading-wrapper">
      <text class="loading-text">加载中...</text>
    </view>
    
    <template v-else-if="station">
      <!-- 头部图片 -->
      <swiper class="station-swiper" indicator-dots autoplay circular>
        <swiper-item v-for="(img, index) in stationImages" :key="index">
          <image class="station-image" :src="img" mode="aspectFill" />
        </swiper-item>
      </swiper>
      
      <!-- 基本信息 -->
      <view class="station-info card">
        <view class="station-header">
          <view class="station-name">{{ station.name }}</view>
          <view class="station-status" :class="statusClass">
            {{ statusText }}
          </view>
        </view>
        
        <view class="station-address" @click="openNavigation">
          <text class="address-icon">📍</text>
          <text class="address-text">{{ station.address }}</text>
          <text class="nav-icon">🧭</text>
        </view>
        
        <view class="station-meta">
          <view class="meta-item">
            <text class="meta-label">营业时间</text>
            <text class="meta-value">{{ station.openTime }} - {{ station.closeTime }}</text>
          </view>
          <view class="meta-item" v-if="station.parkingFeeDesc">
            <text class="meta-label">停车费</text>
            <text class="meta-value">{{ station.parkingFeeDesc }}</text>
          </view>
          <view class="meta-item" v-if="station.phone">
            <text class="meta-label">联系电话</text>
            <text class="meta-value text-primary" @click="callPhone">{{ station.phone }}</text>
          </view>
        </view>
        
        <!-- 设施标签 -->
        <view class="station-facilities" v-if="station.facilities?.length">
          <text 
            v-for="(facility, index) in station.facilities" 
            :key="index"
            class="facility-tag"
          >
            {{ facility }}
          </text>
        </view>
      </view>
      
      <!-- 充电桩列表 -->
      <view class="charger-section">
        <view class="section-header">
          <text class="section-title">充电桩</text>
          <text class="section-subtitle">
            空闲 <text class="text-success">{{ station.availableCount }}</text>/{{ station.chargerCount }}
          </text>
        </view>
        
        <view class="charger-list">
          <view 
            v-for="charger in chargers" 
            :key="charger.id" 
            class="charger-card card"
            @click="selectCharger(charger)"
          >
            <view class="charger-header">
              <view class="charger-name">{{ charger.name }}</view>
              <view class="charger-status" :class="getChargerStatusClass(charger.status)">
                {{ getChargerStatusText(charger.status) }}
              </view>
            </view>
            
            <view class="charger-info">
              <view class="info-item">
                <text class="info-label">类型</text>
                <text class="info-value">{{ charger.type === 'DC' ? '直流快充' : '交流慢充' }}</text>
              </view>
              <view class="info-item">
                <text class="info-label">功率</text>
                <text class="info-value">{{ charger.power }}kW</text>
              </view>
              <view class="info-item">
                <text class="info-label">电价</text>
                <text class="info-value text-primary">¥{{ charger.price.toFixed(2) }}/度</text>
              </view>
            </view>
            
            <!-- 充电枪 -->
            <view class="connector-list">
              <view 
                v-for="connector in charger.connectors" 
                :key="connector.id"
                class="connector-item"
                :class="{ available: connector.status === 'AVAILABLE' }"
              >
                <text class="connector-code">{{ connector.code }}</text>
                <text class="connector-status">{{ getConnectorStatusText(connector.status) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </template>
    
    <!-- 底部操作栏 -->
    <view class="bottom-bar safe-area-bottom" v-if="station">
      <view class="price-info">
        <text class="price-label">电价</text>
        <text class="price-value">¥{{ station.minPrice?.toFixed(2) }}</text>
        <text class="price-unit">/度起</text>
      </view>
      <view class="btn btn-primary" @click="goToScan">
        扫码充电
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores'
import { getStationDetail, getStationChargers } from '@/api/station'
import type { Station, Charger, ChargerStatus, ConnectorStatus } from '@/types'

const userStore = useUserStore()

const stationId = ref<number>(0)
const station = ref<Station | null>(null)
const chargers = ref<Charger[]>([])
const loading = ref(true)

const stationImages = computed(() => {
  if (station.value?.images?.length) {
    return station.value.images
  }
  return ['/static/images/station-default.png']
})

const statusClass = computed(() => {
  switch (station.value?.status) {
    case 'ONLINE': return 'status-online'
    case 'OFFLINE': return 'status-offline'
    case 'MAINTENANCE': return 'status-maintenance'
    default: return ''
  }
})

const statusText = computed(() => {
  switch (station.value?.status) {
    case 'ONLINE': return '营业中'
    case 'OFFLINE': return '已关闭'
    case 'MAINTENANCE': return '维护中'
    default: return ''
  }
})

onLoad((options) => {
  if (options?.id) {
    stationId.value = Number(options.id)
    loadData()
  }
})

onMounted(() => {
  if (stationId.value) {
    loadData()
  }
})

async function loadData() {
  loading.value = true
  try {
    const [stationRes, chargersRes] = await Promise.all([
      getStationDetail(stationId.value),
      getStationChargers(stationId.value)
    ])
    
    station.value = stationRes.data || null
    chargers.value = chargersRes.data || []
  } catch (error) {
    console.error('Failed to load station detail:', error)
  } finally {
    loading.value = false
  }
}

function getChargerStatusClass(status: ChargerStatus) {
  switch (status) {
    case 'AVAILABLE': return 'status-available'
    case 'CHARGING': return 'status-charging'
    case 'OFFLINE': return 'status-offline'
    case 'FAULT': return 'status-fault'
    default: return ''
  }
}

function getChargerStatusText(status: ChargerStatus) {
  switch (status) {
    case 'AVAILABLE': return '空闲'
    case 'CHARGING': return '使用中'
    case 'OFFLINE': return '离线'
    case 'FAULT': return '故障'
    default: return ''
  }
}

function getConnectorStatusText(status: ConnectorStatus) {
  switch (status) {
    case 'AVAILABLE': return '空闲'
    case 'CHARGING': return '充电中'
    case 'OFFLINE': return '离线'
    case 'FAULT': return '故障'
    case 'RESERVED': return '已预约'
    default: return ''
  }
}

function openNavigation() {
  if (!station.value) return
  
  uni.openLocation({
    latitude: station.value.latitude,
    longitude: station.value.longitude,
    name: station.value.name,
    address: station.value.address,
    scale: 18
  })
}

function callPhone() {
  if (!station.value?.phone) return
  
  uni.makePhoneCall({
    phoneNumber: station.value.phone
  })
}

function selectCharger(charger: Charger) {
  if (charger.status !== 'AVAILABLE') {
    uni.showToast({
      title: '该充电桩暂不可用',
      icon: 'none'
    })
    return
  }
  
  // 可以跳转到充电确认页面或显示充电枪选择
  goToScan()
}

function goToScan() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  
  uni.navigateTo({ 
    url: `/pages/charging/scan?stationId=${stationId.value}` 
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.station-swiper {
  width: 100%;
  height: 400rpx;
}

.station-image {
  width: 100%;
  height: 100%;
}

.station-info {
  margin: -40rpx $spacing-lg $spacing-lg;
  position: relative;
  z-index: 10;
}

.station-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-md;
  
  .station-name {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $text-color;
    flex: 1;
  }
  
  .station-status {
    padding: $spacing-xs $spacing-md;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    
    &.status-online {
      background-color: rgba($success-color, 0.1);
      color: $success-color;
    }
    
    &.status-offline {
      background-color: rgba($text-color-tertiary, 0.1);
      color: $text-color-tertiary;
    }
    
    &.status-maintenance {
      background-color: rgba($warning-color, 0.1);
      color: $warning-color;
    }
  }
}

.station-address {
  display: flex;
  align-items: center;
  padding: $spacing-md;
  background-color: $fill-color-light;
  border-radius: $radius-md;
  margin-bottom: $spacing-md;
  
  .address-icon {
    font-size: $font-size-lg;
    margin-right: $spacing-sm;
  }
  
  .address-text {
    flex: 1;
    font-size: $font-size-md;
    color: $text-color-secondary;
  }
  
  .nav-icon {
    font-size: $font-size-xl;
  }
}

.station-meta {
  .meta-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-sm 0;
    border-bottom: 1px solid $border-color-light;
    
    &:last-child {
      border-bottom: none;
    }
    
    .meta-label {
      color: $text-color-tertiary;
      font-size: $font-size-md;
    }
    
    .meta-value {
      color: $text-color;
      font-size: $font-size-md;
    }
  }
}

.station-facilities {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-top: $spacing-md;
  
  .facility-tag {
    padding: $spacing-xs $spacing-md;
    background-color: $fill-color-light;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}

.charger-section {
  padding: 0 $spacing-lg $spacing-lg;
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
  
  .section-subtitle {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}

.charger-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.charger-card {
  padding: $spacing-lg;
}

.charger-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  
  .charger-name {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
  }
  
  .charger-status {
    padding: $spacing-xs $spacing-md;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    
    &.status-available {
      background-color: rgba($success-color, 0.1);
      color: $success-color;
    }
    
    &.status-charging {
      background-color: rgba($info-color, 0.1);
      color: $info-color;
    }
    
    &.status-offline {
      background-color: rgba($text-color-tertiary, 0.1);
      color: $text-color-tertiary;
    }
    
    &.status-fault {
      background-color: rgba($error-color, 0.1);
      color: $error-color;
    }
  }
}

.charger-info {
  display: flex;
  gap: $spacing-xl;
  margin-bottom: $spacing-md;
  
  .info-item {
    display: flex;
    flex-direction: column;
    
    .info-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-bottom: $spacing-xs;
    }
    
    .info-value {
      font-size: $font-size-md;
      color: $text-color;
    }
  }
}

.connector-list {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.connector-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-sm $spacing-md;
  background-color: $fill-color-light;
  border-radius: $radius-md;
  min-width: 100rpx;
  
  &.available {
    background-color: rgba($success-color, 0.1);
    
    .connector-status {
      color: $success-color;
    }
  }
  
  .connector-code {
    font-size: $font-size-md;
    font-weight: 500;
    color: $text-color;
  }
  
  .connector-status {
    font-size: $font-size-xs;
    color: $text-color-tertiary;
    margin-top: $spacing-xs;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
  background-color: $bg-color-white;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  
  .price-info {
    display: flex;
    align-items: baseline;
    
    .price-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-right: $spacing-xs;
    }
    
    .price-value {
      font-size: $font-size-xxl;
      font-weight: 600;
      color: $primary-color;
    }
    
    .price-unit {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
    }
  }
  
  .btn {
    width: 280rpx;
  }
}

.page-container {
  padding-bottom: 160rpx;
}
</style>
