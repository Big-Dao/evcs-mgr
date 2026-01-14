<template>
  <view class="station-card" @click="handleClick">
    <view class="card-header">
      <image 
        class="station-image" 
        :src="station.imageUrl || '/static/images/station-default.png'"
        mode="aspectFill"
      />
      <view class="station-status" :class="statusClass">
        {{ statusText }}
      </view>
    </view>
    
    <view class="card-body">
      <view class="station-name ellipsis">{{ station.name }}</view>
      <view class="station-address ellipsis">{{ station.address }}</view>
      
      <view class="station-info">
        <view class="info-item">
          <text class="info-value text-primary">{{ station.availableCount || 0 }}</text>
          <text class="info-label">/{{ station.totalCount || 0 }}空闲</text>
        </view>
        <view class="info-item" v-if="distance">
          <text class="info-value">{{ formatDistance(distance) }}</text>
        </view>
        <view class="info-item">
          <text class="info-label">¥</text>
          <text class="info-value">{{ station.price?.toFixed(2) || '-' }}</text>
          <text class="info-label">/度</text>
        </view>
      </view>
      
      <view class="station-tags" v-if="station.tags?.length">
        <text 
          v-for="tag in station.tags.slice(0, 3)" 
          :key="tag"
          class="tag"
        >
          {{ tag }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Station } from '@/types'

interface Props {
  station: Station
  distance?: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', station: Station): void
}>()

const statusClass = computed(() => {
  if (!props.station.availableCount) return 'status-busy'
  if (props.station.status === 'OFFLINE') return 'status-offline'
  return 'status-available'
})

const statusText = computed(() => {
  if (props.station.status === 'OFFLINE') return '离线'
  if (!props.station.availableCount) return '繁忙'
  return '空闲'
})

function formatDistance(meters: number): string {
  if (meters < 1000) {
    return `${Math.round(meters)}m`
  }
  return `${(meters / 1000).toFixed(1)}km`
}

function handleClick() {
  emit('click', props.station)
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.station-card {
  background-color: $bg-color-white;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.card-header {
  position: relative;
  height: 200rpx;
  
  .station-image {
    width: 100%;
    height: 100%;
  }
  
  .station-status {
    position: absolute;
    top: $spacing-sm;
    right: $spacing-sm;
    padding: $spacing-xs $spacing-md;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    color: #FFFFFF;
    
    &.status-available {
      background-color: $success-color;
    }
    
    &.status-busy {
      background-color: $warning-color;
    }
    
    &.status-offline {
      background-color: $text-color-tertiary;
    }
  }
}

.card-body {
  padding: $spacing-md;
}

.station-name {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.station-address {
  font-size: $font-size-sm;
  color: $text-color-tertiary;
  margin-bottom: $spacing-md;
}

.station-info {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  margin-bottom: $spacing-sm;
  
  .info-item {
    display: flex;
    align-items: baseline;
    
    .info-value {
      font-size: $font-size-lg;
      font-weight: 500;
      color: $text-color;
    }
    
    .info-label {
      font-size: $font-size-sm;
      color: $text-color-tertiary;
    }
  }
}

.station-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  
  .tag {
    padding: 4rpx $spacing-sm;
    background-color: $fill-color-light;
    border-radius: $radius-sm;
    font-size: $font-size-xs;
    color: $text-color-secondary;
  }
}
</style>
