<template>
  <view class="page-container">
    <view class="login-form">
      <view class="form-title">手机号登录</view>
      
      <!-- 手机号输入 -->
      <view class="form-item">
        <text class="input-prefix">+86</text>
        <input
          class="form-input"
          type="number"
          v-model="phone"
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>
      
      <!-- 验证码输入 -->
      <view class="form-item">
        <input
          class="form-input"
          type="number"
          v-model="code"
          placeholder="请输入验证码"
          maxlength="6"
        />
        <view 
          class="send-code-btn"
          :class="{ disabled: countdown > 0 || !isPhoneValid }"
          @click="sendCode"
        >
          <text>{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</text>
        </view>
      </view>
      
      <!-- 登录按钮 -->
      <view 
        class="btn btn-primary btn-lg login-btn"
        :class="{ 'btn-disabled': !canLogin }"
        @click="handleLogin"
      >
        登录
      </view>
      
      <!-- 其他登录方式 -->
      <view class="other-login">
        <view class="divider-line">
          <text>其他登录方式</text>
        </view>
        <view class="login-methods">
          <!-- #ifdef MP-WEIXIN -->
          <view class="method-item" @click="loginWithWechat">
            <text class="method-icon">💬</text>
            <text class="method-name">微信</text>
          </view>
          <!-- #endif -->
          
          <!-- #ifdef MP-ALIPAY -->
          <view class="method-item" @click="loginWithAlipay">
            <text class="method-icon">💙</text>
            <text class="method-name">支付宝</text>
          </view>
          <!-- #endif -->
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores'
import { sendSmsCode } from '@/api/auth'

const userStore = useUserStore()

const phone = ref('')
const code = ref('')
const countdown = ref(0)

let timer: ReturnType<typeof setInterval> | null = null

const isPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value))
const canLogin = computed(() => isPhoneValid.value && code.value.length === 6)

async function sendCode() {
  if (!isPhoneValid.value || countdown.value > 0) return
  
  try {
    await sendSmsCode({
      phone: phone.value,
      type: 'login'
    })
    
    uni.showToast({
      title: '验证码已发送',
      icon: 'success'
    })
    
    // 开始倒计时
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    console.error('Send code error:', error)
    uni.showToast({
      title: '发送失败',
      icon: 'none'
    })
  }
}

async function handleLogin() {
  if (!canLogin.value) return
  
  uni.showLoading({ title: '登录中...' })
  
  try {
    const success = await userStore.loginByPhoneCode({
      phone: phone.value,
      code: code.value,
      loginType: 'sms'
    })
    
    uni.hideLoading()
    
    if (success) {
      uni.showToast({
        title: '登录成功',
        icon: 'success'
      })
      setTimeout(() => {
        uni.switchTab({ url: '/pages/home/index' })
      }, 1500)
    }
  } catch (error) {
    uni.hideLoading()
    console.error('Login error:', error)
    uni.showToast({
      title: '登录失败',
      icon: 'none'
    })
  }
}

async function loginWithWechat() {
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
  }
}

async function loginWithAlipay() {
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
  }
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';

.login-form {
  padding: $spacing-xxl;
}

.form-title {
  font-size: 48rpx;
  font-weight: 700;
  color: $text-color;
  margin-bottom: $spacing-xxl;
}

.form-item {
  display: flex;
  align-items: center;
  height: 100rpx;
  border-bottom: 2rpx solid $border-color;
  margin-bottom: $spacing-lg;
  
  .input-prefix {
    font-size: $font-size-lg;
    color: $text-color;
    margin-right: $spacing-md;
    padding-right: $spacing-md;
    border-right: 2rpx solid $border-color;
  }
  
  .form-input {
    flex: 1;
    font-size: $font-size-lg;
    color: $text-color;
  }
}

.send-code-btn {
  padding: $spacing-sm $spacing-md;
  
  text {
    font-size: $font-size-md;
    color: $primary-color;
  }
  
  &.disabled text {
    color: $text-color-tertiary;
  }
}

.login-btn {
  margin-top: $spacing-xxl;
}

.other-login {
  margin-top: 80rpx;
}

.divider-line {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-xl;
  
  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background-color: $border-color;
  }
  
  text {
    padding: 0 $spacing-lg;
    font-size: $font-size-sm;
    color: $text-color-tertiary;
  }
}

.login-methods {
  display: flex;
  justify-content: center;
  gap: 80rpx;
}

.method-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .method-icon {
    font-size: 64rpx;
    margin-bottom: $spacing-sm;
  }
  
  .method-name {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}
</style>
