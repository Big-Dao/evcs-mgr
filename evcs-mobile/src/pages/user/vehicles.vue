<template>
  <view class="page-container">
    <view class="empty-state" v-if="vehicles.length === 0">
      <text class="empty-icon">🚗</text>
      <text class="empty-text">暂未绑定车辆</text>
      <view class="btn btn-primary empty-action" @click="addVehicle">
        添加车辆
      </view>
    </view>
    
    <view v-else class="vehicle-list">
      <view 
        v-for="vehicle in vehicles" 
        :key="vehicle.id"
        class="vehicle-card card"
      >
        <view class="vehicle-header">
          <text class="vehicle-plate">{{ vehicle.plateNumber }}</text>
          <view class="vehicle-default" v-if="vehicle.isDefault">
            <text>默认</text>
          </view>
        </view>
        <view class="vehicle-info">
          <text class="vehicle-brand">{{ vehicle.brand }} {{ vehicle.model }}</text>
        </view>
        <view class="vehicle-actions">
          <text class="action-btn" @click="editVehicle(vehicle)">编辑</text>
          <text class="action-btn text-error" @click="deleteVehicle(vehicle)">删除</text>
        </view>
      </view>
    </view>
    
    <!-- 添加按钮 -->
    <view class="add-btn-wrapper" v-if="vehicles.length > 0">
      <view class="btn btn-primary btn-lg" @click="addVehicle">
        添加车辆
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Vehicle {
  id: number
  plateNumber: string
  brand: string
  model: string
  isDefault: boolean
}

const vehicles = ref<Vehicle[]>([])

onMounted(() => {
  loadVehicles()
})

function loadVehicles() {
  // 模拟数据
  vehicles.value = []
}

function addVehicle() {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

function editVehicle(vehicle: Vehicle) {
  console.log('Edit vehicle:', vehicle)
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}

function deleteVehicle(vehicle: Vehicle) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除车辆 ${vehicle.plateNumber} 吗？`,
    success: (res) => {
      if (res.confirm) {
        // 删除车辆
        vehicles.value = vehicles.value.filter(v => v.id !== vehicle.id)
        uni.showToast({
          title: '删除成功',
          icon: 'success'
        })
      }
    }
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.vehicle-list {
  padding: $spacing-lg;
}

.vehicle-card {
  margin-bottom: $spacing-md;
}

.vehicle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
  
  .vehicle-plate {
    font-size: $font-size-xl;
    font-weight: 600;
    color: $text-color;
  }
  
  .vehicle-default {
    padding: $spacing-xs $spacing-md;
    background-color: rgba($primary-color, 0.1);
    border-radius: $radius-full;
    
    text {
      font-size: $font-size-sm;
      color: $primary-color;
    }
  }
}

.vehicle-info {
  .vehicle-brand {
    font-size: $font-size-md;
    color: $text-color-secondary;
  }
}

.vehicle-actions {
  display: flex;
  gap: $spacing-lg;
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color-light;
  
  .action-btn {
    font-size: $font-size-md;
    color: $primary-color;
  }
}

.add-btn-wrapper {
  padding: $spacing-lg;
  padding-bottom: calc(#{$spacing-lg} + constant(safe-area-inset-bottom));
  padding-bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx $spacing-lg;
  
  .empty-icon {
    font-size: 160rpx;
    margin-bottom: $spacing-xl;
  }
  
  .empty-text {
    font-size: $font-size-lg;
    color: $text-color-tertiary;
    margin-bottom: $spacing-xl;
  }
  
  .empty-action {
    width: 300rpx;
  }
}
</style>
