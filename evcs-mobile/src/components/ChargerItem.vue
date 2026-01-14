<template>
  <view class="charger-item" :class="{ disabled: !isAvailable }" @click="handleClick">
    <view class="charger-header">
      <view class="charger-icon">
        <text>{{ charger.connectorType === 'DC' ? '⚡' : '🔌' }}</text>
      </view>
      <view class="charger-info">
        <text class="charger-name">{{ charger.code }}</text>
        <text class="charger-type">{{ getConnectorTypeText(charger.connectorType) }}</text>
      </view>
      <view class="charger-status" :class="statusClass">
        {{ statusText }}
      </view>
    </view>
    
    <view class="charger-specs">
      <view class="spec-item">
        <text class="spec-label">额定功率</text>
        <text class="spec-value">{{ charger.power || '-' }}kW</text>
      </view>
      <view class="spec-item">
        <text class="spec-label">电压</text>
        <text class="spec-value">{{ charger.voltage || '-' }}V</text>
      </view>
      <view class="spec-item">
        <text class="spec-label">电流</text>
        <text class="spec-value">{{ charger.current || '-' }}A</text>
      </view>
    </view>
    
    <view class="charger-connectors" v-if="charger.connectors?.length">
      <view 
        v-for="connector in charger.connectors" 
        :key="connector.id"
        class="connector-item"
        :class="{ available: connector.status === 'AVAILABLE' }"
      >
        <text class="connector-code">{{ connector.code }}</text>
        <text class="connector-status">{{ getConnectorStatus(connector.status) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Charger, ConnectorStatus } from '@/types'

interface Props {
  charger: Charger
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', charger: Charger): void
}>()

const isAvailable = computed(() => {
  return props.charger.status === 'AVAILABLE' || 
         props.charger.connectors?.some(c => c.status === 'AVAILABLE')
})

const statusClass = computed(() => {
  switch (props.charger.status) {
    case 'AVAILABLE': return 'status-available'
    case 'CHARGING': return 'status-charging'
    case 'OCCUPIED': return 'status-occupied'
    case 'FAULTED': return 'status-faulted'
    default: return 'status-offline'
  }
})

const statusText = computed(() => {
  switch (props.charger.status) {
    case 'AVAILABLE': return '空闲'
    case 'CHARGING': return '充电中'
    case 'OCCUPIED': return '占用'
    case 'FAULTED': return '故障'
    default: return '离线'
  }
})

function getConnectorTypeText(type: string): string {
  switch (type) {
    case 'DC': return '直流快充'
    case 'AC': return '交流慢充'
    default: return type
  }
}

function getConnectorStatus(status: ConnectorStatus): string {
  switch (status) {
    case 'AVAILABLE': return '空闲'
    case 'CHARGING': return '充电中'
    case 'OCCUPIED': return '占用'
    case 'FAULTED': return '故障'
    default: return '离线'
  }
}

function handleClick() {
  if (isAvailable.value) {
    emit('click', props.charger)
  }
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.charger-item {
  background-color: $bg-color-white;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-sm;
  
  &.disabled {
    opacity: 0.6;
  }
}

.charger-header {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
}

.charger-icon {
  width: 80rpx;
  height: 80rpx;
  background-color: $fill-color-light;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  margin-right: $spacing-md;
}

.charger-info {
  flex: 1;
  
  .charger-name {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $text-color;
    display: block;
  }
  
  .charger-type {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
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
  
  &.status-occupied {
    background-color: rgba($warning-color, 0.1);
    color: $warning-color;
  }
  
  &.status-faulted {
    background-color: rgba($error-color, 0.1);
    color: $error-color;
  }
  
  &.status-offline {
    background-color: rgba($text-color-tertiary, 0.1);
    color: $text-color-tertiary;
  }
}

.charger-specs {
  display: flex;
  justify-content: space-between;
  padding: $spacing-md 0;
  border-top: 1px solid $border-color-light;
  border-bottom: 1px solid $border-color-light;
  margin-bottom: $spacing-md;
  
  .spec-item {
    text-align: center;
    
    .spec-label {
      display: block;
      font-size: $font-size-sm;
      color: $text-color-tertiary;
      margin-bottom: $spacing-xs;
    }
    
    .spec-value {
      font-size: $font-size-md;
      font-weight: 500;
      color: $text-color;
    }
  }
}

.charger-connectors {
  display: flex;
  gap: $spacing-md;
}

.connector-item {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-sm $spacing-md;
  background-color: $fill-color-light;
  border-radius: $radius-md;
  
  &.available {
    background-color: rgba($success-color, 0.1);
    
    .connector-status {
      color: $success-color;
    }
  }
  
  .connector-code {
    font-size: $font-size-md;
    color: $text-color;
  }
  
  .connector-status {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}
</style>
