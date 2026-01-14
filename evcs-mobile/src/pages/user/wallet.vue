<template>
  <view class="page-container">
    <!-- 余额卡片 -->
    <view class="balance-card">
      <view class="balance-header">
        <text class="balance-label">账户余额(元)</text>
      </view>
      <view class="balance-amount">
        <text class="amount-value">{{ walletInfo?.balance?.toFixed(2) || '0.00' }}</text>
      </view>
      <view class="balance-frozen" v-if="walletInfo?.frozenAmount">
        <text>冻结金额: ¥{{ walletInfo.frozenAmount.toFixed(2) }}</text>
      </view>
    </view>
    
    <!-- 充值按钮 -->
    <view class="recharge-section">
      <view class="btn btn-primary btn-lg" @click="showRechargeModal = true">
        立即充值
      </view>
    </view>
    
    <!-- 统计数据 -->
    <view class="stats-section card">
      <view class="stat-item">
        <text class="stat-value">¥{{ walletInfo?.totalRecharge?.toFixed(2) || '0.00' }}</text>
        <text class="stat-label">累计充值</text>
      </view>
      <view class="stat-divider"></view>
      <view class="stat-item">
        <text class="stat-value">¥{{ walletInfo?.totalConsume?.toFixed(2) || '0.00' }}</text>
        <text class="stat-label">累计消费</text>
      </view>
    </view>
    
    <!-- 充值记录 -->
    <view class="records-section">
      <view class="section-header">
        <text class="section-title">充值记录</text>
      </view>
      
      <view v-if="loading" class="loading-wrapper">
        <text class="loading-text">加载中...</text>
      </view>
      
      <view v-else-if="records.length === 0" class="empty-state">
        <text class="empty-text">暂无充值记录</text>
      </view>
      
      <view v-else class="record-list">
        <view 
          v-for="record in records" 
          :key="record.id"
          class="record-item card"
        >
          <view class="record-info">
            <text class="record-title">账户充值</text>
            <text class="record-time">{{ formatDateTime(record.createdAt) }}</text>
          </view>
          <view class="record-amount">
            <text class="amount text-success">+¥{{ record.amount.toFixed(2) }}</text>
            <text class="status" :class="getStatusClass(record.status)">
              {{ getStatusText(record.status) }}
            </text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 充值弹窗 -->
    <view class="recharge-modal" v-if="showRechargeModal" @click.self="showRechargeModal = false">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">账户充值</text>
          <text class="modal-close" @click="showRechargeModal = false">✕</text>
        </view>
        
        <view class="modal-body">
          <!-- 金额选择 -->
          <view class="amount-options">
            <view 
              v-for="option in amountOptions" 
              :key="option"
              class="amount-option"
              :class="{ active: selectedAmount === option }"
              @click="selectAmount(option)"
            >
              <text class="option-amount">¥{{ option }}</text>
            </view>
          </view>
          
          <!-- 自定义金额 -->
          <view class="custom-amount">
            <text class="custom-label">自定义金额</text>
            <input
              class="custom-input"
              type="digit"
              v-model="customAmount"
              placeholder="请输入金额"
              @input="onCustomInput"
            />
          </view>
          
          <!-- 支付方式 -->
          <view class="payment-methods">
            <text class="method-label">选择支付方式</text>
            <view 
              class="method-item"
              :class="{ active: paymentMethod === 'WECHAT' }"
              @click="paymentMethod = 'WECHAT'"
            >
              <text class="method-icon">💬</text>
              <text class="method-name">微信支付</text>
              <text class="method-check">{{ paymentMethod === 'WECHAT' ? '✓' : '' }}</text>
            </view>
            <view 
              class="method-item"
              :class="{ active: paymentMethod === 'ALIPAY' }"
              @click="paymentMethod = 'ALIPAY'"
            >
              <text class="method-icon">💙</text>
              <text class="method-name">支付宝</text>
              <text class="method-check">{{ paymentMethod === 'ALIPAY' ? '✓' : '' }}</text>
            </view>
          </view>
        </view>
        
        <view class="modal-footer">
          <view class="btn btn-primary btn-lg" @click="doRecharge">
            确认充值 ¥{{ finalAmount }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getWalletInfo, getRechargeRecords, createRechargeOrder, invokeWechatPay, invokeAlipay } from '@/api/payment'
import type { WalletInfo, RechargeRecord } from '@/api/payment'
import type { PaymentMethod } from '@/types'
import { formatDateTime } from '@/utils/format'

const walletInfo = ref<WalletInfo | null>(null)
const records = ref<RechargeRecord[]>([])
const loading = ref(false)

const showRechargeModal = ref(false)
const amountOptions = [50, 100, 200, 500]
const selectedAmount = ref<number | null>(100)
const customAmount = ref('')
const paymentMethod = ref<PaymentMethod>('WECHAT')

const finalAmount = computed(() => {
  if (customAmount.value) {
    return Number(customAmount.value) || 0
  }
  return selectedAmount.value || 0
})

onMounted(() => {
  loadWalletInfo()
  loadRecords()
})

async function loadWalletInfo() {
  try {
    const res = await getWalletInfo()
    walletInfo.value = res.data || null
  } catch (error) {
    console.error('Failed to load wallet info:', error)
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getRechargeRecords()
    records.value = res.data || []
  } catch (error) {
    console.error('Failed to load records:', error)
  } finally {
    loading.value = false
  }
}

function selectAmount(amount: number) {
  selectedAmount.value = amount
  customAmount.value = ''
}

function onCustomInput() {
  if (customAmount.value) {
    selectedAmount.value = null
  }
}

function getStatusClass(status: string) {
  switch (status) {
    case 'SUCCESS': return 'text-success'
    case 'FAILED': return 'text-error'
    default: return 'text-warning'
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'SUCCESS': return '成功'
    case 'FAILED': return '失败'
    default: return '处理中'
  }
}

async function doRecharge() {
  if (finalAmount.value <= 0) {
    uni.showToast({
      title: '请选择充值金额',
      icon: 'none'
    })
    return
  }
  
  uni.showLoading({ title: '请求支付...' })
  
  try {
    const res = await createRechargeOrder(finalAmount.value, paymentMethod.value)
    
    if (res.data?.payParams) {
      if (paymentMethod.value === 'WECHAT') {
        await invokeWechatPay(res.data.payParams)
      } else if (paymentMethod.value === 'ALIPAY') {
        await invokeAlipay(res.data.payParams)
      }
      
      showRechargeModal.value = false
      
      // 刷新数据
      await loadWalletInfo()
      await loadRecords()
      
      uni.showToast({
        title: '充值成功',
        icon: 'success'
      })
    }
  } catch (error) {
    console.error('Recharge error:', error)
    uni.showToast({
      title: '充值失败',
      icon: 'none'
    })
  } finally {
    uni.hideLoading()
  }
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.balance-card {
  background: linear-gradient(135deg, $primary-color 0%, $primary-color-dark 100%);
  padding: $spacing-xxl;
  color: #FFFFFF;
  
  .balance-header {
    .balance-label {
      font-size: $font-size-md;
      opacity: 0.8;
    }
  }
  
  .balance-amount {
    margin-top: $spacing-md;
    
    .amount-value {
      font-size: 72rpx;
      font-weight: 700;
    }
  }
  
  .balance-frozen {
    margin-top: $spacing-sm;
    font-size: $font-size-sm;
    opacity: 0.7;
  }
}

.recharge-section {
  padding: $spacing-lg;
  margin-top: -40rpx;
  position: relative;
  z-index: 10;
}

.stats-section {
  margin: 0 $spacing-lg $spacing-lg;
  display: flex;
  align-items: center;
  
  .stat-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    
    .stat-value {
      font-size: $font-size-xl;
      font-weight: 600;
      color: $text-color;
    }
    
    .stat-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-top: $spacing-xs;
    }
  }
  
  .stat-divider {
    width: 1px;
    height: 60rpx;
    background-color: $border-color-light;
  }
}

.records-section {
  padding: 0 $spacing-lg;
}

.section-header {
  margin-bottom: $spacing-md;
  
  .section-title {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $text-color;
  }
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md $spacing-lg;
}

.record-info {
  .record-title {
    font-size: $font-size-md;
    color: $text-color;
    display: block;
  }
  
  .record-time {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
    margin-top: $spacing-xs;
  }
}

.record-amount {
  text-align: right;
  
  .amount {
    font-size: $font-size-lg;
    font-weight: 500;
    display: block;
  }
  
  .status {
    font-size: $font-size-sm;
    margin-top: $spacing-xs;
  }
}

.recharge-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: $mask-color;
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.modal-content {
  width: 100%;
  background-color: $bg-color-white;
  border-radius: $radius-xl $radius-xl 0 0;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 1px solid $border-color-light;
  
  .modal-title {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
  }
  
  .modal-close {
    font-size: $font-size-xl;
    color: $text-color-tertiary;
    padding: $spacing-sm;
  }
}

.modal-body {
  padding: $spacing-lg;
}

.amount-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-xl;
}

.amount-option {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
  background-color: $fill-color-light;
  border-radius: $radius-md;
  border: 2rpx solid transparent;
  
  &.active {
    background-color: rgba($primary-color, 0.1);
    border-color: $primary-color;
    
    .option-amount {
      color: $primary-color;
    }
  }
  
  .option-amount {
    font-size: $font-size-lg;
    font-weight: 500;
    color: $text-color;
  }
}

.custom-amount {
  margin-bottom: $spacing-xl;
  
  .custom-label {
    font-size: $font-size-md;
    color: $text-color-secondary;
    margin-bottom: $spacing-sm;
    display: block;
  }
  
  .custom-input {
    width: 100%;
    height: 88rpx;
    border: 2rpx solid $border-color;
    border-radius: $radius-md;
    padding: 0 $spacing-lg;
    font-size: $font-size-lg;
    
    &:focus {
      border-color: $primary-color;
    }
  }
}

.payment-methods {
  .method-label {
    font-size: $font-size-md;
    color: $text-color-secondary;
    margin-bottom: $spacing-md;
    display: block;
  }
}

.method-item {
  display: flex;
  align-items: center;
  padding: $spacing-lg;
  border: 2rpx solid $border-color-light;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  
  &.active {
    border-color: $primary-color;
    background-color: rgba($primary-color, 0.05);
  }
  
  .method-icon {
    font-size: 40rpx;
    margin-right: $spacing-md;
  }
  
  .method-name {
    flex: 1;
    font-size: $font-size-lg;
    color: $text-color;
  }
  
  .method-check {
    color: $primary-color;
    font-size: $font-size-xl;
  }
}

.modal-footer {
  padding: $spacing-lg;
  padding-bottom: calc(#{$spacing-lg} + constant(safe-area-inset-bottom));
  padding-bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));
}
</style>
