<template>
  <view class="page-container">
    <!-- 充电动画区域 -->
    <view class="charging-header">
      <view class="charging-animation">
        <view class="charging-circle outer"></view>
        <view class="charging-circle middle"></view>
        <view class="charging-circle inner"></view>
        <view class="charging-icon">⚡</view>
      </view>
      <text class="charging-status">充电中</text>
    </view>
    
    <!-- 实时数据 -->
    <view class="data-section">
      <view class="data-card main-data">
        <view class="data-item large">
          <text class="data-value">{{ formatEnergy(session?.energy || 0) }}</text>
          <text class="data-label">已充电量</text>
        </view>
        <view class="data-divider"></view>
        <view class="data-item large">
          <text class="data-value">¥{{ (session?.currentAmount || 0).toFixed(2) }}</text>
          <text class="data-label">当前费用</text>
        </view>
      </view>
      
      <view class="data-card detail-data">
        <view class="detail-row">
          <view class="detail-item">
            <text class="detail-label">充电时长</text>
            <text class="detail-value">{{ formatDuration(session?.duration || 0) }}</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">实时功率</text>
            <text class="detail-value">{{ session?.power || 0 }}kW</text>
          </view>
        </view>
        <view class="detail-row">
          <view class="detail-item">
            <text class="detail-label">电压</text>
            <text class="detail-value">{{ session?.voltage || 0 }}V</text>
          </view>
          <view class="detail-item">
            <text class="detail-label">电流</text>
            <text class="detail-value">{{ session?.current || 0 }}A</text>
          </view>
        </view>
        <view class="detail-row" v-if="session?.soc">
          <view class="detail-item">
            <text class="detail-label">电池电量</text>
            <text class="detail-value text-primary">{{ session.soc }}%</text>
          </view>
          <view class="detail-item" v-if="session?.estimatedEndTime">
            <text class="detail-label">预计充满</text>
            <text class="detail-value">{{ formatTime(session.estimatedEndTime) }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 订单信息 -->
    <view class="order-info card">
      <view class="info-row">
        <text class="info-label">订单号</text>
        <text class="info-value">{{ order?.orderNo || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">充电站</text>
        <text class="info-value">{{ order?.stationName || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">充电桩</text>
        <text class="info-value">{{ order?.chargerCode || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="info-label">开始时间</text>
        <text class="info-value">{{ formatDateTime(order?.startTime || '') }}</text>
      </view>
    </view>
    
    <!-- 底部操作 -->
    <view class="bottom-actions safe-area-bottom">
      <view class="btn btn-primary btn-lg" @click="handleStopCharging">
        结束充电
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useChargingStore } from '@/stores'
import { formatEnergy, formatDuration, formatDateTime } from '@/utils/format'

const chargingStore = useChargingStore()

const order = computed(() => chargingStore.activeOrder)
const session = computed(() => chargingStore.chargingSession)

onMounted(() => {
  if (!chargingStore.activeOrder) {
    chargingStore.checkActiveCharging()
  } else {
    chargingStore.startPolling()
  }
})

onShow(() => {
  if (chargingStore.activeOrder && !chargingStore.isPolling) {
    chargingStore.startPolling()
  }
})

onHide(() => {
  // 页面隐藏时不停止轮询，让用户可以继续在后台跟踪
})

onUnmounted(() => {
  // 组件销毁时停止轮询
  chargingStore.stopPolling()
})

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

async function handleStopCharging() {
  uni.showModal({
    title: '确认结束充电',
    content: '确定要结束当前充电吗？',
    confirmColor: '#00B42A',
    success: async (res) => {
      if (res.confirm) {
        await stopCharging()
      }
    }
  })
}

async function stopCharging() {
  uni.showLoading({ title: '正在结束...' })
  
  try {
    const completedOrder = await chargingStore.stopCharging()
    
    uni.hideLoading()
    uni.showToast({
      title: '充电已结束',
      icon: 'success'
    })
    
    // 跳转到订单详情页面
    setTimeout(() => {
      if (completedOrder) {
        uni.redirectTo({ 
          url: `/pages/order/detail?id=${completedOrder.id}` 
        })
      } else {
        uni.switchTab({ url: '/pages/order/list' })
      }
    }, 1500)
  } catch (error) {
    uni.hideLoading()
    uni.showToast({
      title: '结束失败，请重试',
      icon: 'none'
    })
  }
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, $primary-color 0%, #E8F8EB 50%, $bg-color 100%);
  padding-bottom: 160rpx;
}

.charging-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0 60rpx;
}

.charging-animation {
  position: relative;
  width: 300rpx;
  height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.charging-circle {
  position: absolute;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  
  &.outer {
    width: 100%;
    height: 100%;
    animation: pulse 2s ease-in-out infinite;
  }
  
  &.middle {
    width: 80%;
    height: 80%;
    animation: pulse 2s ease-in-out infinite 0.3s;
  }
  
  &.inner {
    width: 60%;
    height: 60%;
    background-color: rgba(255, 255, 255, 0.2);
    animation: pulse 2s ease-in-out infinite 0.6s;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

.charging-icon {
  position: relative;
  z-index: 1;
  font-size: 100rpx;
  animation: flash 1s ease-in-out infinite;
}

@keyframes flash {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.charging-status {
  margin-top: $spacing-lg;
  font-size: $font-size-xl;
  font-weight: 600;
  color: #FFFFFF;
}

.data-section {
  padding: 0 $spacing-lg;
}

.data-card {
  background-color: $bg-color-white;
  border-radius: $radius-xl;
  padding: $spacing-xl;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-md;
}

.main-data {
  display: flex;
  align-items: center;
  
  .data-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    
    &.large .data-value {
      font-size: 64rpx;
      font-weight: 700;
      color: $primary-color;
    }
    
    .data-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-top: $spacing-sm;
    }
  }
  
  .data-divider {
    width: 2rpx;
    height: 80rpx;
    background-color: $border-color-light;
  }
}

.detail-data {
  .detail-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: $spacing-md;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .detail-item {
    flex: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    &:first-child {
      margin-right: $spacing-xl;
    }
    
    .detail-label {
      font-size: $font-size-md;
      color: $text-color-tertiary;
    }
    
    .detail-value {
      font-size: $font-size-md;
      color: $text-color;
      font-weight: 500;
    }
  }
}

.order-info {
  margin: 0 $spacing-lg;
  
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: $spacing-sm 0;
    border-bottom: 1px solid $border-color-light;
    
    &:last-child {
      border-bottom: none;
    }
    
    .info-label {
      font-size: $font-size-md;
      color: $text-color-tertiary;
    }
    
    .info-value {
      font-size: $font-size-md;
      color: $text-color;
    }
  }
}

.bottom-actions {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-lg;
  background-color: $bg-color-white;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}
</style>
