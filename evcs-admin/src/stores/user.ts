import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, type LoginRequest, type LoginUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<LoginUserInfo | null>(null)
  
  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const userId = computed(() => userInfo.value?.id)

  // Actions
  async function login(loginForm: LoginRequest) {
    try {
      const response = await loginApi(loginForm)
      const data = response.data
      
      if (data && data.accessToken) {
        setToken(data.accessToken)
        if (data.user) {
          setUserInfo(data.user)
        }
        return true
      }
      return false
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.warn('Logout API failed:', error)
    } finally {
      clearState()
    }
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(user: LoginUserInfo) {
    userInfo.value = user
    localStorage.setItem('userInfo', JSON.stringify(user))
    if (user.id) {
      localStorage.setItem('userId', String(user.id))
    }
    if (user.tenantId) {
      localStorage.setItem('tenantId', String(user.tenantId))
    }
  }

  function clearState() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userId')
    localStorage.removeItem('tenantId')
    localStorage.removeItem('tokenType')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresIn')
  }

  // Initialize state from localStorage if available
  function init() {
    const storedUser = localStorage.getItem('userInfo')
    if (storedUser) {
      try {
        userInfo.value = JSON.parse(storedUser)
      } catch (e) {
        console.error('Failed to parse stored user info', e)
      }
    }
  }

  init()

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    userId,
    login,
    logout,
    setToken,
    setUserInfo,
    clearState
  }
})
