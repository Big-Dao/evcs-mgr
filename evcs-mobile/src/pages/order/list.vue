<template>
  <view class="page-container">
    <!-- 顶部标签栏 -->
    <view class="tabs">
      <view 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>
    
    <!-- 订单列表 -->
    <scroll-view 
      class="order-scroll"
      scroll-y
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
    >
      <view v-if="loading && orders.length === 0" class="loading-wrapper">
        <text class="loading-text">加载中...</text>
      </view>
      
      <view v-else-if="orders.length === 0" class="empty-state">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无订单</text>
      </view>
      
      <view v-else class="order-list">
        <view 
          v-for="order in orders" 
          :key="order.id"
          class="order-card card"
          @click="goToDetail(order.id)"
        >
          <view class="order-header">
            <view class="station-info">
              <text class="station-name ellipsis">{{ order.stationName }}</text>
              <text class="charger-code">{{ order.chargerCode }}</text>
            </view>
            <view class="order-status" :class="getStatusClass(order.status)">
              {{ getStatusText(order.status) }}
            </view>
          </view>
          
          <view class="order-content">
            <view class="order-data">
              <view class="data-item">
                <text class="data-label">充电量</text>
                <text class="data-value">{{ formatEnergy(order.energy || 0) }}</text>
              </view>
              <view class="data-item">
                <text class="data-label">时长</text>
                <text class="data-value">{{ formatDuration(order.duration || 0) }}</text>
              </view>
              <view class="data-item">
                <text class="data-label">费用</text>
                <text class="data-value text-primary">¥{{ (order.totalAmount || 0).toFixed(2) }}</text>
              </view>
            </view>
          </view>
          
          <view class="order-footer">
            <text class="order-time">{{ formatDateTime(order.createdAt) }}</text>
            <view class="order-actions">
              <view 
                v-if="order.status === 'COMPLETED' && order.paymentStatus === 'UNPAID'"
                class="btn btn-primary btn-sm"
                @click.stop="handlePay(order)"
              >
                去支付
              </view>
              <view 
                v-else-if="order.status === 'CHARGING'"
                class="btn btn-outline btn-sm"
                @click.stop="goToSession"
              >
                查看充电
              </view>
            </view>
          </view>
        </view>
        
        <view v-if="loading" class="loading-more">
          <text>加载中...</text>
        </view>
        
        <view v-if="noMore && orders.length > 0" class="no-more">
          <text>没有更多了</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores'
import { getOrders } from '@/api/order'
import { formatEnergy, formatDuration, formatDateTime } from '@/utils/format'
import type { Order, OrderStatus } from '@/types'

const userStore = useUserStore()

const tabs = [
  { label: '全部', value: '' },
  { label: '充电中', value: 'CHARGING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '待支付', value: 'UNPAID' }
]

const currentTab = ref<string>('')
const orders = ref<Order[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const currentPage = ref(1)
const pageSize = 10

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadOrders()
  }
})

onShow(() => {
  if (userStore.isLoggedIn) {
    loadOrders(true)
  }
})

async function loadOrders(isRefresh = false) {
  if (loading.value) return
  
  if (isRefresh) {
    currentPage.value = 1
    noMore.value = false
  }
  
  loading.value = true
  
  try {
    const params: Record<string, unknown> = {
      current: currentPage.value,
      size: pageSize
    }
    
    if (currentTab.value === 'UNPAID') {
      // 待支付：已完成但未支付
      params.status = 'COMPLETED'
    } else if (currentTab.value) {
      params.status = currentTab.value
    }
    
    const res = await getOrders(params as any)
    
    if (res.data) {
      let newOrders = res.data.records || []
      
      // 如果是待支付标签，过滤出未支付的订单
      if (currentTab.value === 'UNPAID') {
        newOrders = newOrders.filter(o => o.paymentStatus === 'UNPAID')
      }
      
      if (isRefresh) {
        orders.value = newOrders
      } else {
        orders.value = [...orders.value, ...newOrders]
      }
      
      if (newOrders.length < pageSize) {
        noMore.value = true
      }
    }
  } catch (error) {
    console.error('Failed to load orders:', error)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function switchTab(value: string) {
  currentTab.value = value
  loadOrders(true)
}

async function onRefresh() {
  refreshing.value = true
  await loadOrders(true)
}

async function onLoadMore() {
  if (noMore.value || loading.value) return
  currentPage.value++
  await loadOrders()
}

function getStatusClass(status: OrderStatus) {
  switch (status) {
    case 'CHARGING': return 'status-charging'
    case 'COMPLETED': return 'status-completed'
    case 'CANCELLED': return 'status-cancelled'
    case 'FAILED': return 'status-failed'
    default: return ''
  }
}

function getStatusText(status: OrderStatus) {
  switch (status) {
    case 'PENDING': return '待充电'
    case 'CHARGING': return '充电中'
    case 'COMPLETED': return '已完成'
    case 'CANCELLED': return '已取消'
    case 'FAILED': return '充电失败'
    default: return ''
  }
}

function goToDetail(orderId: number) {
  uni.navigateTo({ url: `/pages/order/detail?id=${orderId}` })
}

function goToSession() {
  uni.navigateTo({ url: '/pages/charging/session' })
}

function handlePay(order: Order) {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.id}&action=pay` })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.tabs {
  display: flex;
  background-color: $bg-color-white;
  border-bottom: 1px solid $border-color-light;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  font-size: $font-size-md;
  color: $text-color-secondary;
  position: relative;
  
  &.active {
    color: $primary-color;
    font-weight: 500;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 48rpx;
      height: 6rpx;
      background-color: $primary-color;
      border-radius: $radius-full;
    }
  }
}

.order-scroll {
  flex: 1;
  height: calc(100vh - 88rpx);
}

.order-list {
  padding: $spacing-md $spacing-lg;
}

.order-card {
  margin-bottom: $spacing-md;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $border-color-light;
}

.station-info {
  flex: 1;
  min-width: 0;
  
  .station-name {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
    display: block;
    margin-bottom: $spacing-xs;
  }
  
  .charger-code {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}

.order-status {
  padding: $spacing-xs $spacing-md;
  border-radius: $radius-full;
  font-size: $font-size-sm;
  flex-shrink: 0;
  
  &.status-charging {
    background-color: rgba($info-color, 0.1);
    color: $info-color;
  }
  
  &.status-completed {
    background-color: rgba($success-color, 0.1);
    color: $success-color;
  }
  
  &.status-cancelled {
    background-color: rgba($text-color-tertiary, 0.1);
    color: $text-color-tertiary;
  }
  
  &.status-failed {
    background-color: rgba($error-color, 0.1);
    color: $error-color;
  }
}

.order-content {
  margin-bottom: $spacing-md;
}

.order-data {
  display: flex;
  justify-content: space-between;
  
  .data-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .data-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-bottom: $spacing-xs;
    }
    
    .data-value {
      font-size: $font-size-lg;
      font-weight: 500;
      color: $text-color;
    }
  }
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .order-time {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}

.order-actions {
  display: flex;
  gap: $spacing-sm;
}

.loading-more,
.no-more {
  text-align: center;
  padding: $spacing-lg;
  color: $text-color-tertiary;
  font-size: $font-size-sm;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  
  .empty-icon {
    font-size: 120rpx;
    margin-bottom: $spacing-lg;
  }
  
  .empty-text {
    color: $text-color-tertiary;
    font-size: $font-size-md;
  }
}
</style>
