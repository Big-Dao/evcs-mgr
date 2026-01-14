<template>
  <view class="price-display">
    <view class="price-row" v-if="electricityPrice">
      <text class="price-label">电费</text>
      <text class="price-value">¥{{ electricityPrice.toFixed(4) }}/度</text>
    </view>
    <view class="price-row" v-if="servicePrice">
      <text class="price-label">服务费</text>
      <text class="price-value">¥{{ servicePrice.toFixed(4) }}/度</text>
    </view>
    <view class="price-row total" v-if="showTotal">
      <text class="price-label">合计</text>
      <text class="price-value text-primary">¥{{ totalPrice.toFixed(4) }}/度</text>
    </view>
    
    <view class="time-periods" v-if="timePeriods?.length">
      <view class="period-title">分时电价</view>
      <view 
        v-for="(period, index) in timePeriods" 
        :key="index"
        class="period-item"
        :class="{ current: isCurrentPeriod(period) }"
      >
        <text class="period-time">{{ period.startTime }}-{{ period.endTime }}</text>
        <text class="period-type">{{ period.type }}</text>
        <text class="period-price">¥{{ period.price.toFixed(4) }}/度</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface TimePeriod {
  startTime: string
  endTime: string
  type: string
  price: number
}

interface Props {
  electricityPrice?: number
  servicePrice?: number
  timePeriods?: TimePeriod[]
  showTotal?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showTotal: true
})

const totalPrice = computed(() => {
  return (props.electricityPrice || 0) + (props.servicePrice || 0)
})

function isCurrentPeriod(period: TimePeriod): boolean {
  const now = new Date()
  const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  return currentTime >= period.startTime && currentTime < period.endTime
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.price-display {
  background-color: $bg-color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm 0;
  
  &.total {
    margin-top: $spacing-sm;
    padding-top: $spacing-md;
    border-top: 1px solid $border-color-light;
    
    .price-label,
    .price-value {
      font-weight: 600;
    }
  }
  
  .price-label {
    font-size: $font-size-md;
    color: $text-color-secondary;
  }
  
  .price-value {
    font-size: $font-size-md;
    color: $text-color;
  }
}

.time-periods {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color-light;
}

.period-title {
  font-size: $font-size-md;
  font-weight: 500;
  color: $text-color;
  margin-bottom: $spacing-md;
}

.period-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;
  
  &.current {
    background-color: rgba($primary-color, 0.05);
    margin: 0 #{-$spacing-lg};
    padding-left: $spacing-lg;
    padding-right: $spacing-lg;
    border-left: 4rpx solid $primary-color;
  }
  
  .period-time {
    flex: 1;
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
  
  .period-type {
    width: 80rpx;
    font-size: $font-size-sm;
    color: $text-color-tertiary;
    text-align: center;
  }
  
  .period-price {
    font-size: $font-size-sm;
    color: $text-color;
    font-weight: 500;
  }
}
</style>
