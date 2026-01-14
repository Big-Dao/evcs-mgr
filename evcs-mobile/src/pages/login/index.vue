<template>
  <view class="page-container">
    <!-- 背景装饰 -->
    <view class="bg-decoration"></view>
    
    <!-- Logo -->
    <view class="logo-section">
      <view class="logo">
        <text class="logo-icon">⚡</text>
      </view>
      <text class="app-name">EVCS充电</text>
      <text class="app-slogan">让充电更简单</text>
    </view>
    
    <!-- 登录方式 -->
    <view class="login-section">
      <!-- 微信一键登录 -->
      <!-- #ifdef MP-WEIXIN -->
      <button class="btn btn-primary btn-lg wechat-btn" @click="loginWithWechat">
        <text class="btn-icon">💬</text>
        <text>微信一键登录</text>
      </button>
      <!-- #endif -->
      
      <!-- 支付宝一键登录 -->
      <!-- #ifdef MP-ALIPAY -->
      <button class="btn btn-primary btn-lg alipay-btn" @click="loginWithAlipay">
        <text class="btn-icon">💙</text>
        <text>支付宝一键登录</text>
      </button>
      <!-- #endif -->
      
      <!-- 手机号登录 -->
      <view class="btn btn-secondary btn-lg" @click="goToPhoneLogin">
        手机号登录
      </view>
      
      <!-- 游客模式 -->
      <view class="guest-entry" @click="skipLogin">
        <text>暂不登录，先看看</text>
      </view>
    </view>
    
    <!-- 协议 -->
    <view class="agreement-section">
      <view class="agreement-check" @click="toggleAgreement">
        <view class="checkbox" :class="{ checked: agreed }">
          <text v-if="agreed">✓</text>
        </view>
        <text class="agreement-text">
          登录即表示同意
          <text class="link" @click.stop="openUserAgreement">《用户协议》</text>
          和
          <text class="link" @click.stop="openPrivacyPolicy">《隐私政策》</text>
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores'

const userStore = useUserStore()

const agreed = ref(false)

function toggleAgreement() {
  agreed.value = !agreed.value
}

function checkAgreement(): boolean {
  if (!agreed.value) {
    uni.showToast({
      title: '请先同意用户协议',
      icon: 'none'
    })
    return false
  }
  return true
}

async function loginWithWechat() {
  if (!checkAgreement()) return
  
  uni.showLoading({ title: '登录中...' })
  
  try {
    await userStore.loginWithWechat()
    uni.hideLoading()
    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 1500)
  } catch (error) {
    uni.hideLoading()
    console.error('Wechat login error:', error)
    uni.showToast({
      title: '登录失败',
      icon: 'none'
    })
  }
}

async function loginWithAlipay() {
  if (!checkAgreement()) return
  
  uni.showLoading({ title: '登录中...' })
  
  try {
    await userStore.loginWithAlipay()
    uni.hideLoading()
    uni.showToast({
      title: '登录成功',
      icon: 'success'
    })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 1500)
  } catch (error) {
    uni.hideLoading()
    console.error('Alipay login error:', error)
    uni.showToast({
      title: '登录失败',
      icon: 'none'
    })
  }
}

function goToPhoneLogin() {
  if (!checkAgreement()) return
  uni.navigateTo({ url: '/pages/login/phone' })
}

function skipLogin() {
  uni.switchTab({ url: '/pages/home/index' })
}

function openUserAgreement() {
  uni.showToast({
    title: '用户协议',
    icon: 'none'
  })
}

function openPrivacyPolicy() {
  uni.showToast({
    title: '隐私政策',
    icon: 'none'
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.page-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: $bg-color-white;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: -200rpx;
  right: -200rpx;
  width: 600rpx;
  height: 600rpx;
  background: radial-gradient(circle, rgba($primary-color, 0.1) 0%, transparent 70%);
  border-radius: 50%;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  padding-bottom: 80rpx;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  background: linear-gradient(135deg, $primary-color 0%, $primary-color-dark 100%);
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-lg;
  
  .logo-icon {
    font-size: 80rpx;
  }
}

.app-name {
  font-size: 48rpx;
  font-weight: 700;
  color: $text-color;
  margin-bottom: $spacing-sm;
}

.app-slogan {
  font-size: $font-size-md;
  color: $text-color-tertiary;
}

.login-section {
  flex: 1;
  padding: 0 $spacing-xxl;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
}

.wechat-btn {
  background: #07C160 !important;
  
  .btn-icon {
    margin-right: $spacing-sm;
  }
}

.alipay-btn {
  background: #1677FF !important;
  
  .btn-icon {
    margin-right: $spacing-sm;
  }
}

.guest-entry {
  text-align: center;
  padding: $spacing-lg;
  
  text {
    font-size: $font-size-md;
    color: $text-color-tertiary;
  }
}

.agreement-section {
  padding: $spacing-xl;
  padding-bottom: calc(#{$spacing-xl} + constant(safe-area-inset-bottom));
  padding-bottom: calc(#{$spacing-xl} + env(safe-area-inset-bottom));
}

.agreement-check {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid $border-color;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: $spacing-sm;
  margin-top: 4rpx;
  flex-shrink: 0;
  
  &.checked {
    background-color: $primary-color;
    border-color: $primary-color;
    
    text {
      color: #FFFFFF;
      font-size: $font-size-sm;
    }
  }
}

.agreement-text {
  font-size: $font-size-sm;
  color: $text-color-tertiary;
  line-height: 1.5;
  
  .link {
    color: $primary-color;
  }
}
</style>
