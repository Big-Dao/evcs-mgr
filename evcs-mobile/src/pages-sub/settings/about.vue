<template>
  <view class="page-container">
    <!-- Logo -->
    <view class="about-header">
      <view class="logo">
        <text class="logo-icon">⚡</text>
      </view>
      <text class="app-name">EVCS充电</text>
      <text class="app-version">版本 v1.0.0</text>
    </view>
    
    <!-- 公司信息 -->
    <view class="about-section card">
      <view class="section-title">关于我们</view>
      <view class="section-content">
        <text>
          EVCS充电是一款专业的电动汽车充电服务平台，致力于为车主提供便捷、高效、安全的充电体验。
        </text>
        <text class="mt-md">
          我们拥有覆盖全国的充电网络，支持多种充电桩类型，提供实时充电监控和多种支付方式。
        </text>
      </view>
    </view>
    
    <!-- 联系方式 -->
    <view class="about-section card">
      <view class="section-title">联系我们</view>
      <view class="contact-list">
        <view class="contact-item">
          <text class="contact-label">客服电话</text>
          <text class="contact-value" @click="callService">400-XXX-XXXX</text>
        </view>
        <view class="contact-item">
          <text class="contact-label">客服邮箱</text>
          <text class="contact-value">service@evcs.com</text>
        </view>
        <view class="contact-item">
          <text class="contact-label">官方网站</text>
          <text class="contact-value">www.evcs.com</text>
        </view>
      </view>
    </view>
    
    <!-- 法律信息 -->
    <view class="legal-links">
      <text class="link" @click="openUserAgreement">用户协议</text>
      <text class="divider">|</text>
      <text class="link" @click="openPrivacyPolicy">隐私政策</text>
    </view>
    
    <view class="copyright">
      <text>© 2024 EVCS. All rights reserved.</text>
    </view>
  </view>
</template>

<script setup lang="ts">
function callService() {
  uni.makePhoneCall({
    phoneNumber: '400XXXXXXX'
  })
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

.about-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: $spacing-xxl;
  background-color: $bg-color-white;
}

.logo {
  width: 120rpx;
  height: 120rpx;
  background: linear-gradient(135deg, $primary-color 0%, $primary-color-dark 100%);
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-md;
  
  .logo-icon {
    font-size: 60rpx;
  }
}

.app-name {
  font-size: $font-size-xl;
  font-weight: 600;
  color: $text-color;
}

.app-version {
  font-size: $font-size-sm;
  color: $text-color-tertiary;
  margin-top: $spacing-xs;
}

.about-section {
  margin: $spacing-lg;
}

.section-title {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-color;
  margin-bottom: $spacing-md;
}

.section-content {
  text {
    font-size: $font-size-md;
    color: $text-color-secondary;
    line-height: 1.6;
    display: block;
  }
}

.contact-list {
  .contact-item {
    display: flex;
    justify-content: space-between;
    padding: $spacing-sm 0;
    
    .contact-label {
      font-size: $font-size-md;
      color: $text-color-tertiary;
    }
    
    .contact-value {
      font-size: $font-size-md;
      color: $primary-color;
    }
  }
}

.legal-links {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: $spacing-xl;
  
  .link {
    font-size: $font-size-md;
    color: $primary-color;
  }
  
  .divider {
    margin: 0 $spacing-md;
    color: $border-color;
  }
}

.copyright {
  text-align: center;
  padding-bottom: $spacing-xxl;
  
  text {
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}
</style>
