<template>
  <div class="test-page">
    <h1>认证测试页面</h1>
    <p>Token状态: {{ tokenStatus }}</p>
    <p>当前时间: {{ currentTime }}</p>

    <div style="margin-top: 20px;">
      <h3>API测试</h3>
      <el-button @click="testDashboardAPI" type="primary" :loading="loading">测试Dashboard API</el-button>
      <el-button @click="testUserInfoAPI" type="success" :loading="loading">测试UserInfo API</el-button>
      <el-button @click="testLogout" type="danger">测试退出</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const tokenStatus = ref('检查中...')
const currentTime = ref('')
const loading = ref(false)

onMounted(() => {
  // 检查token状态
  const token = localStorage.getItem('token')
  tokenStatus.value = token ? 'Token存在' : 'Token不存在'
  currentTime.value = new Date().toLocaleString()

  console.log('Test页面挂载 - Token状态:', tokenStatus.value)
  console.log('Test页面挂载 - Token内容:', token?.substring(0, 50) + '...')
})

const testDashboardAPI = async () => {
  loading.value = true
  try {
    console.log('开始测试Dashboard API...')
    const response = await request({
      url: '/dashboard/statistics',
      method: 'get'
    })
    console.log('Dashboard API 成功:', response)
  } catch (error) {
    console.error('Dashboard API 失败:', error)
  } finally {
    loading.value = false
  }
}

const testUserInfoAPI = async () => {
  loading.value = true
  try {
    console.log('开始测试UserInfo API...')
    const response = await request({
      url: '/auth/userinfo',
      method: 'get'
    })
    console.log('UserInfo API 成功:', response)
  } catch (error) {
    console.error('UserInfo API 失败:', error)
  } finally {
    loading.value = false
  }
}

const testLogout = () => {
  console.log('手动测试退出')
  localStorage.removeItem('token')
  tokenStatus.value = 'Token已清除'
}
</script>

<style scoped>
.test-page {
  padding: 20px;
}
</style>