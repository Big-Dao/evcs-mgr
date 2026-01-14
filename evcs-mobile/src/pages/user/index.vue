<template>
  <view class="page-container">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="user-info" @click="goToProfile">
        <image 
          class="user-avatar" 
          :src="avatar || '/static/images/avatar-default.png'"
          mode="aspectFill"
        />
        <view class="user-detail">
          <text class="user-name">{{ isLoggedIn ? nickname : '点击登录' }}</text>
          <text class="user-phone" v-if="isLoggedIn">{{ formatPhone(phone) }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
      
      <view class="user-stats" v-if="isLoggedIn">
        <view class="stat-item" @click="goToOrders">
          <text class="stat-value">{{ orderCount }}</text>
          <text class="stat-label">充电次数</text>
        </view>
        <view class="stat-item" @click="goToWallet">
          <text class="stat-value">¥{{ balance.toFixed(2) }}</text>
          <text class="stat-label">账户余额</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ totalEnergy.toFixed(1) }}</text>
          <text class="stat-label">累计充电(度)</text>
        </view>
      </view>
    </view>
    
    <!-- 功能菜单 -->
    <view class="menu-section card">
      <view class="menu-item" @click="goToWallet">
        <text class="menu-icon">💰</text>
        <text class="menu-text">我的钱包</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToVehicles">
        <text class="menu-icon">🚗</text>
        <text class="menu-text">我的车辆</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToFavorites">
        <text class="menu-icon">⭐</text>
        <text class="menu-text">收藏站点</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToCoupons">
        <text class="menu-icon">🎫</text>
        <text class="menu-text">优惠券</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
    
    <view class="menu-section card">
      <view class="menu-item" @click="goToHelp">
        <text class="menu-icon">❓</text>
        <text class="menu-text">帮助中心</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToAbout">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goToSettings">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
    
    <!-- 退出登录 -->
    <view class="logout-section" v-if="isLoggedIn">
      <view class="btn btn-secondary btn-lg" @click="handleLogout">
        退出登录
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores'
import { formatPhone } from '@/utils/format'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const nickname = computed(() => userStore.nickname || '用户')
const phone = computed(() => userStore.phone)
const avatar = computed(() => userStore.avatar)
const balance = computed(() => userStore.balance)

// 模拟数据（实际应从后端获取）
const orderCount = ref(0)
const totalEnergy = ref(0)

onMounted(() => {
  if (isLoggedIn.value) {
    loadUserStats()
  }
})

onShow(() => {
  if (isLoggedIn.value) {
    userStore.refreshUserInfo()
  }
})

function loadUserStats() {
  // 从后端获取用户统计数据
  // 这里先用模拟数据
  orderCount.value = 12
  totalEnergy.value = 156.8
}

function goToProfile() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/user/profile' })
}

function goToOrders() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.switchTab({ url: '/pages/order/list' })
}

function goToWallet() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/user/wallet' })
}

function goToVehicles() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.navigateTo({ url: '/pages/user/vehicles' })
}

function goToFavorites() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

function goToCoupons() {
  if (!isLoggedIn.value) {
    uni.navigateTo({ url: '/pages/login/index' })
    return
  }
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

function goToHelp() {
  uni.navigateTo({ url: '/pages-sub/settings/help' })
}

function goToAbout() {
  uni.navigateTo({ url: '/pages-sub/settings/about' })
}

function goToSettings() {
  uni.navigateTo({ url: '/pages-sub/settings/index' })
}

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
      }
    }
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.user-card {
  background: linear-gradient(135deg, $primary-color 0%, $primary-color-dark 100%);
  padding: $spacing-xl $spacing-lg;
  padding-top: calc(#{$spacing-xl} + constant(safe-area-inset-top));
  padding-top: calc(#{$spacing-xl} + env(safe-area-inset-top));
}

.user-info {
  display: flex;
  align-items: center;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
}

.user-detail {
  flex: 1;
  margin-left: $spacing-lg;
  
  .user-name {
    font-size: $font-size-xl;
    font-weight: 600;
    color: #FFFFFF;
    display: block;
  }
  
  .user-phone {
    font-size: $font-size-sm;
    color: rgba(255, 255, 255, 0.8);
    margin-top: $spacing-xs;
  }
}

.arrow {
  font-size: $font-size-xxl;
  color: rgba(255, 255, 255, 0.6);
}

.user-stats {
  display: flex;
  justify-content: space-around;
  margin-top: $spacing-xl;
  padding-top: $spacing-lg;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .stat-value {
    font-size: $font-size-xl;
    font-weight: 600;
    color: #FFFFFF;
  }
  
  .stat-label {
    font-size: $font-size-sm;
    color: rgba(255, 255, 255, 0.8);
    margin-top: $spacing-xs;
  }
}

.menu-section {
  margin: $spacing-lg;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: $spacing-lg 0;
  border-bottom: 1px solid $border-color-light;
  
  &:last-child {
    border-bottom: none;
  }
  
  .menu-icon {
    font-size: 40rpx;
    margin-right: $spacing-md;
  }
  
  .menu-text {
    flex: 1;
    font-size: $font-size-lg;
    color: $text-color;
  }
  
  .menu-arrow {
    font-size: $font-size-xl;
    color: $text-color-tertiary;
  }
}

.logout-section {
  padding: $spacing-xl $spacing-lg;
}
</style>
