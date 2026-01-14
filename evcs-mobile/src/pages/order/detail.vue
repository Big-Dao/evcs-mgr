<template>
  <view class="page-container">
    <view v-if="loading" class="loading-wrapper">
      <text class="loading-text">加载中...</text>
    </view>
    
    <template v-else-if="order">
      <!-- 订单状态 -->
      <view class="status-header" :class="statusClass">
        <text class="status-icon">{{ statusIcon }}</text>
        <text class="status-text">{{ statusText }}</text>
        <text class="status-desc" v-if="statusDesc">{{ statusDesc }}</text>
      </view>
      
      <!-- 充电信息 -->
      <view class="info-section card">
        <view class="section-title">充电信息</view>
        
        <view class="data-grid">
          <view class="data-item">
            <text class="data-value">{{ formatEnergy(order.energy || 0) }}</text>
            <text class="data-label">充电量</text>
          </view>
          <view class="data-item">
            <text class="data-value">{{ formatDuration(order.duration || 0) }}</text>
            <text class="data-label">充电时长</text>
          </view>
        </view>
        
        <view class="divider"></view>
        
        <view class="info-list">
          <view class="info-row">
            <text class="info-label">充电站</text>
            <text class="info-value">{{ order.stationName }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">充电桩</text>
            <text class="info-value">{{ order.chargerCode }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">充电枪</text>
            <text class="info-value">{{ order.connectorCode }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">开始时间</text>
            <text class="info-value">{{ formatDateTime(order.startTime || '') }}</text>
          </view>
          <view class="info-row" v-if="order.endTime">
            <text class="info-label">结束时间</text>
            <text class="info-value">{{ formatDateTime(order.endTime) }}</text>
          </view>
        </view>
      </view>
      
      <!-- 费用明细 -->
      <view class="fee-section card">
        <view class="section-title">费用明细</view>
        
        <view class="fee-list">
          <view class="fee-row">
            <text class="fee-label">电费</text>
            <text class="fee-value">¥{{ (order.amount || 0).toFixed(2) }}</text>
          </view>
          <view class="fee-row" v-if="order.serviceFee">
            <text class="fee-label">服务费</text>
            <text class="fee-value">¥{{ order.serviceFee.toFixed(2) }}</text>
          </view>
        </view>
        
        <view class="fee-total">
          <text class="total-label">合计</text>
          <text class="total-value">¥{{ (order.totalAmount || 0).toFixed(2) }}</text>
        </view>
      </view>
      
      <!-- 订单信息 -->
      <view class="order-section card">
        <view class="section-title">订单信息</view>
        
        <view class="info-list">
          <view class="info-row">
            <text class="info-label">订单号</text>
            <text class="info-value">{{ order.orderNo }}</text>
            <text class="copy-btn" @click="copyOrderNo">复制</text>
          </view>
          <view class="info-row">
            <text class="info-label">下单时间</text>
            <text class="info-value">{{ formatDateTime(order.createdAt) }}</text>
          </view>
          <view class="info-row" v-if="order.paymentTime">
            <text class="info-label">支付时间</text>
            <text class="info-value">{{ formatDateTime(order.paymentTime) }}</text>
          </view>
          <view class="info-row" v-if="order.paymentMethod">
            <text class="info-label">支付方式</text>
            <text class="info-value">{{ getPaymentMethodText(order.paymentMethod) }}</text>
          </view>
        </view>
      </view>
      
      <!-- 底部操作 -->
      <view class="bottom-actions safe-area-bottom" v-if="showActions">
        <view 
          v-if="order.paymentStatus === 'UNPAID'" 
          class="btn btn-primary btn-lg"
          @click="handlePay"
        >
          立即支付 ¥{{ (order.totalAmount || 0).toFixed(2) }}
        </view>
        <view 
          v-else-if="order.status === 'COMPLETED' && !order.rating"
          class="btn btn-outline btn-lg"
          @click="handleRate"
        >
          评价订单
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getOrderDetail } from '@/api/order'
import { createOrderPayment, invokeWechatPay, invokeAlipay } from '@/api/payment'
import { formatEnergy, formatDuration, formatDateTime } from '@/utils/format'
import type { Order, PaymentMethod } from '@/types'

const orderId = ref<number>(0)
const order = ref<Order | null>(null)
const loading = ref(true)

const statusClass = computed(() => {
  switch (order.value?.status) {
    case 'COMPLETED': return 'status-success'
    case 'CHARGING': return 'status-charging'
    case 'CANCELLED': return 'status-cancelled'
    case 'FAILED': return 'status-error'
    default: return ''
  }
})

const statusIcon = computed(() => {
  switch (order.value?.status) {
    case 'COMPLETED': return '✓'
    case 'CHARGING': return '⚡'
    case 'CANCELLED': return '✕'
    case 'FAILED': return '!'
    default: return ''
  }
})

const statusText = computed(() => {
  if (order.value?.status === 'COMPLETED' && order.value?.paymentStatus === 'UNPAID') {
    return '待支付'
  }
  
  switch (order.value?.status) {
    case 'PENDING': return '待充电'
    case 'CHARGING': return '充电中'
    case 'COMPLETED': return '充电完成'
    case 'CANCELLED': return '已取消'
    case 'FAILED': return '充电失败'
    default: return ''
  }
})

const statusDesc = computed(() => {
  if (order.value?.status === 'COMPLETED' && order.value?.paymentStatus === 'UNPAID') {
    return '请尽快完成支付'
  }
  return ''
})

const showActions = computed(() => {
  if (!order.value) return false
  return order.value.paymentStatus === 'UNPAID' || 
         (order.value.status === 'COMPLETED' && !order.value.rating)
})

onLoad((options) => {
  if (options?.id) {
    orderId.value = Number(options.id)
    loadOrder()
  }
})

onMounted(() => {
  if (orderId.value) {
    loadOrder()
  }
})

async function loadOrder() {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId.value)
    order.value = res.data || null
  } catch (error) {
    console.error('Failed to load order:', error)
  } finally {
    loading.value = false
  }
}

function getPaymentMethodText(method: PaymentMethod) {
  switch (method) {
    case 'WECHAT': return '微信支付'
    case 'ALIPAY': return '支付宝'
    case 'BALANCE': return '余额支付'
    case 'CARD': return '银行卡'
    default: return ''
  }
}

function copyOrderNo() {
  if (!order.value) return
  
  uni.setClipboardData({
    data: order.value.orderNo,
    success: () => {
      uni.showToast({
        title: '已复制',
        icon: 'success'
      })
    }
  })
}

async function handlePay() {
  if (!order.value) return
  
  // 选择支付方式
  uni.showActionSheet({
    itemList: ['微信支付', '支付宝', '余额支付'],
    success: async (res) => {
      const methods: PaymentMethod[] = ['WECHAT', 'ALIPAY', 'BALANCE']
      const method = methods[res.tapIndex]
      await doPay(method)
    }
  })
}

async function doPay(method: PaymentMethod) {
  if (!order.value) return
  
  uni.showLoading({ title: '支付中...' })
  
  try {
    const res = await createOrderPayment(order.value.id, method)
    
    if (res.data?.payParams) {
      if (method === 'WECHAT') {
        await invokeWechatPay(res.data.payParams)
      } else if (method === 'ALIPAY') {
        await invokeAlipay(res.data.payParams)
      }
      
      // 支付成功，刷新订单
      await loadOrder()
      uni.showToast({
        title: '支付成功',
        icon: 'success'
      })
    } else if (method === 'BALANCE') {
      // 余额支付直接完成
      await loadOrder()
      uni.showToast({
        title: '支付成功',
        icon: 'success'
      })
    }
  } catch (error) {
    console.error('Payment error:', error)
    uni.showToast({
      title: '支付失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}

function handleRate() {
  // 评价功能
  uni.showToast({
    title: '评价功能开发中',
    icon: 'none'
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.page-container {
  padding-bottom: 160rpx;
}

.status-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
  
  &.status-success {
    background: linear-gradient(180deg, rgba($success-color, 0.1) 0%, transparent 100%);
    .status-icon { background-color: $success-color; }
    .status-text { color: $success-color; }
  }
  
  &.status-charging {
    background: linear-gradient(180deg, rgba($info-color, 0.1) 0%, transparent 100%);
    .status-icon { background-color: $info-color; }
    .status-text { color: $info-color; }
  }
  
  &.status-cancelled {
    background: linear-gradient(180deg, rgba($text-color-tertiary, 0.1) 0%, transparent 100%);
    .status-icon { background-color: $text-color-tertiary; }
    .status-text { color: $text-color-tertiary; }
  }
  
  &.status-error {
    background: linear-gradient(180deg, rgba($error-color, 0.1) 0%, transparent 100%);
    .status-icon { background-color: $error-color; }
    .status-text { color: $error-color; }
  }
  
  .status-icon {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #FFFFFF;
    font-size: 56rpx;
    font-weight: bold;
    margin-bottom: $spacing-md;
  }
  
  .status-text {
    font-size: $font-size-xl;
    font-weight: 600;
  }
  
  .status-desc {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
    margin-top: $spacing-xs;
  }
}

.info-section,
.fee-section,
.order-section {
  margin: $spacing-lg;
}

.section-title {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
  margin-bottom: $spacing-lg;
}

.data-grid {
  display: flex;
  justify-content: space-around;
  margin-bottom: $spacing-lg;
  
  .data-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .data-value {
      font-size: 48rpx;
      font-weight: 700;
      color: $primary-color;
    }
    
    .data-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-top: $spacing-xs;
    }
  }
}

.info-list {
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm 0;
    
    .info-label {
      color: $text-color-tertiary;
      font-size: $font-size-md;
    }
    
    .info-value {
      color: $text-color;
      font-size: $font-size-md;
      flex: 1;
      text-align: right;
      margin-left: $spacing-md;
    }
    
    .copy-btn {
      color: $primary-color;
      font-size: $font-size-sm;
      margin-left: $spacing-sm;
      padding: $spacing-xs $spacing-sm;
    }
  }
}

.fee-list {
  .fee-row {
    display: flex;
    justify-content: space-between;
    padding: $spacing-sm 0;
    
    .fee-label {
      color: $text-color-secondary;
      font-size: $font-size-md;
    }
    
    .fee-value {
      color: $text-color;
      font-size: $font-size-md;
    }
  }
}

.fee-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: $spacing-md;
  margin-top: $spacing-md;
  border-top: 1px solid $border-color-light;
  
  .total-label {
    font-size: $font-size-lg;
    color: $text-color;
  }
  
  .total-value {
    font-size: 48rpx;
    font-weight: 700;
    color: $primary-color;
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
