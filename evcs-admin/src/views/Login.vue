<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="logo-section">
        <el-icon style="font-size: 48px; color: #409eff;"><Lightning /></el-icon>
        <h1>EVCS Manager</h1>
        <p>充电站管理系统</p>
      </div>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="tenantId">
          <el-input
            v-model.number="loginForm.tenantId"
            placeholder="租户ID (默认为1)"
            prefix-icon="Key"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { login } from '../api/auth'
import type { LoginResponse } from '../api/auth'

const router = useRouter()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: 'admin',
  password: 'password',
  tenantId: 1 // 默认租户ID
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  tenantId: [
    { required: true, message: '请输入租户ID', trigger: 'blur' },
    { type: 'number', message: '租户ID必须为数字', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 调用真实登录API
        const response = await login({
          username: loginForm.username,
          password: loginForm.password,
          tenantId: loginForm.tenantId
        })
        
        const payload: LoginResponse | undefined = response?.data

        // 保存token和用户信息
        if (payload?.accessToken) {
          localStorage.setItem('token', payload.accessToken)
          localStorage.setItem('tokenType', payload.tokenType ?? 'Bearer')

          if (payload.refreshToken) {
            localStorage.setItem('refreshToken', payload.refreshToken)
          } else {
            localStorage.removeItem('refreshToken')
          }

          if (typeof payload.expiresIn === 'number') {
            localStorage.setItem('tokenExpiresIn', String(payload.expiresIn))
          } else {
            localStorage.removeItem('tokenExpiresIn')
          }

          const user = payload.user
          if (user && user.id !== undefined && user.id !== null) {
            localStorage.setItem('userId', String(user.id))
          } else {
            localStorage.removeItem('userId')
          }

          const tenantId = user?.tenantId ?? loginForm.tenantId
          if (tenantId !== undefined && tenantId !== null) {
            localStorage.setItem('tenantId', String(tenantId))
          } else {
            localStorage.removeItem('tenantId')
          }

          const username = user?.username ?? loginForm.username
          localStorage.setItem('username', username)

          if (user?.realName) {
            localStorage.setItem('realName', user.realName)
          } else {
            localStorage.removeItem('realName')
          }

          ElMessage.success(response?.message ?? '登录成功')
          router.push('/')
        } else {
          ElMessage.error('登录失败：未获取到token')
        }
      } catch (error: any) {
        console.error('登录失败:', error)
        // 错误消息已由 request.ts 拦截器统一处理，这里不再重复显示
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 20px;
}

.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-section h1 {
  margin: 10px 0 5px 0;
  font-size: 28px;
  color: #303133;
}

.logo-section p {
  color: #909399;
  font-size: 14px;
}
</style>
