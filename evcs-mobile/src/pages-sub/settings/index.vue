<template>
  <view class="page-container">
    <view class="settings-list card">
      <view class="setting-item">
        <text class="setting-label">消息通知</text>
        <switch 
          :checked="notifications" 
          @change="toggleNotifications"
          color="#00B42A"
        />
      </view>
      <view class="setting-item">
        <text class="setting-label">自动充满停止</text>
        <switch 
          :checked="autoStop" 
          @change="toggleAutoStop"
          color="#00B42A"
        />
      </view>
      <view class="setting-item" @click="clearCache">
        <text class="setting-label">清除缓存</text>
        <view class="setting-value">
          <text>{{ cacheSize }}</text>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>
    
    <view class="settings-list card">
      <view class="setting-item" @click="goToHelp">
        <text class="setting-label">帮助中心</text>
        <text class="arrow">›</text>
      </view>
      <view class="setting-item" @click="goToAbout">
        <text class="setting-label">关于我们</text>
        <text class="arrow">›</text>
      </view>
      <view class="setting-item" @click="goToFeedback">
        <text class="setting-label">意见反馈</text>
        <text class="arrow">›</text>
      </view>
    </view>
    
    <view class="version-info">
      <text>版本号 v1.0.0</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const notifications = ref(true)
const autoStop = ref(true)
const cacheSize = ref('0KB')

onMounted(() => {
  calculateCacheSize()
})

function calculateCacheSize() {
  // 获取缓存大小
  uni.getStorageInfo({
    success: (res) => {
      const size = res.currentSize
      if (size > 1024) {
        cacheSize.value = `${(size / 1024).toFixed(2)}MB`
      } else {
        cacheSize.value = `${size}KB`
      }
    }
  })
}

function toggleNotifications(e: { detail: { value: boolean } }) {
  notifications.value = e.detail.value
  uni.setStorageSync('notifications', notifications.value)
}

function toggleAutoStop(e: { detail: { value: boolean } }) {
  autoStop.value = e.detail.value
  uni.setStorageSync('autoStop', autoStop.value)
}

function clearCache() {
  uni.showModal({
    title: '确认清除',
    content: '确定要清除缓存吗？',
    success: (res) => {
      if (res.confirm) {
        uni.clearStorage({
          success: () => {
            cacheSize.value = '0KB'
            uni.showToast({
              title: '清除成功',
              icon: 'success'
            })
          }
        })
      }
    }
  })
}

function goToHelp() {
  uni.navigateTo({ url: '/pages-sub/settings/help' })
}

function goToAbout() {
  uni.navigateTo({ url: '/pages-sub/settings/about' })
}

function goToFeedback() {
  uni.showToast({
    title: '功能开发中',
    icon: 'none'
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.settings-list {
  margin: $spacing-lg;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-lg 0;
  border-bottom: 1px solid $border-color-light;
  
  &:last-child {
    border-bottom: none;
  }
  
  .setting-label {
    font-size: $font-size-lg;
    color: $text-color;
  }
  
  .setting-value {
    display: flex;
    align-items: center;
    
    text {
      font-size: $font-size-md;
      color: $text-color-tertiary;
    }
  }
  
  .arrow {
    font-size: $font-size-xl;
    color: $text-color-tertiary;
    margin-left: $spacing-sm;
  }
}

.version-info {
  text-align: center;
  padding: $spacing-xxl;
  
  text {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}
</style>
