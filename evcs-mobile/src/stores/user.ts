import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, LoginRequest } from '@/types'
import { loginByPhone, loginByWechat, loginByAlipay, logout as logoutApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(uni.getStorageSync('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  
  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id)
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  const phone = computed(() => userInfo.value?.phone || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const balance = computed(() => userInfo.value?.balance || 0)
  
  // Actions
  
  /**
   * 初始化用户状态
   */
  function init() {
    const storedToken = uni.getStorageSync('token')
    const storedUserInfo = uni.getStorageSync('userInfo')
    
    if (storedToken) {
      token.value = storedToken
    }
    
    if (storedUserInfo) {
      try {
        userInfo.value = JSON.parse(storedUserInfo)
      } catch (e) {
        console.error('Failed to parse stored user info', e)
      }
    }
    
    // 如果有 token，刷新用户信息
    if (token.value) {
      refreshUserInfo()
    }
  }
  
  /**
   * 手机号登录
   */
  async function loginByPhoneCode(data: LoginRequest) {
    const res = await loginByPhone(data)
    if (res.data) {
      setToken(res.data.accessToken)
      setUserInfo(res.data.user)
      return true
    }
    return false
  }
  
  /**
   * 微信登录
   */
  async function loginWithWechat() {
    // #ifdef MP-WEIXIN
    return new Promise<boolean>((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            const res = await loginByWechat(loginRes.code)
            if (res.data) {
              setToken(res.data.accessToken)
              setUserInfo(res.data.user)
              resolve(true)
            } else {
              resolve(false)
            }
          } catch (error) {
            reject(error)
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
    // #endif
    
    // #ifndef MP-WEIXIN
    throw new Error('当前环境不支持微信登录')
    // #endif
  }
  
  /**
   * 支付宝登录
   */
  async function loginWithAlipay() {
    // #ifdef MP-ALIPAY
    return new Promise<boolean>((resolve, reject) => {
      uni.login({
        provider: 'alipay',
        success: async (loginRes) => {
          try {
            const res = await loginByAlipay(loginRes.code)
            if (res.data) {
              setToken(res.data.accessToken)
              setUserInfo(res.data.user)
              resolve(true)
            } else {
              resolve(false)
            }
          } catch (error) {
            reject(error)
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
    // #endif
    
    // #ifndef MP-ALIPAY
    throw new Error('当前环境不支持支付宝登录')
    // #endif
  }
  
  /**
   * 登出
   */
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.warn('Logout API failed:', error)
    } finally {
      clearState()
      uni.reLaunch({
        url: '/pages/login/index'
      })
    }
  }
  
  /**
   * 刷新用户信息
   */
  async function refreshUserInfo() {
    try {
      const res = await getUserInfo()
      if (res.data) {
        setUserInfo(res.data)
      }
    } catch (error) {
      console.error('Failed to refresh user info:', error)
      // Token 可能已过期
      if ((error as { message?: string })?.message?.includes('未授权')) {
        clearState()
      }
    }
  }
  
  /**
   * 设置 Token
   */
  function setToken(newToken: string) {
    token.value = newToken
    uni.setStorageSync('token', newToken)
  }
  
  /**
   * 设置用户信息
   */
  function setUserInfo(user: UserInfo) {
    userInfo.value = user
    uni.setStorageSync('userInfo', JSON.stringify(user))
    
    if (user.tenantId) {
      uni.setStorageSync('tenantId', String(user.tenantId))
    }
  }
  
  /**
   * 更新余额
   */
  function updateBalance(newBalance: number) {
    if (userInfo.value) {
      userInfo.value.balance = newBalance
      uni.setStorageSync('userInfo', JSON.stringify(userInfo.value))
    }
  }
  
  /**
   * 清空状态
   */
  function clearState() {
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('tenantId')
  }
  
  return {
    // State
    token,
    userInfo,
    // Getters
    isLoggedIn,
    userId,
    nickname,
    phone,
    avatar,
    balance,
    // Actions
    init,
    loginByPhoneCode,
    loginWithWechat,
    loginWithAlipay,
    logout,
    refreshUserInfo,
    setToken,
    setUserInfo,
    updateBalance,
    clearState
  }
})
