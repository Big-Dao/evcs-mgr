import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// Create axios instance
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// Request interceptor
service.interceptors.request.use(
  (config) => {
    // Add token if available
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    
    // 后端返回格式: { success: boolean, message: string, data: any }
    if (res.success === false) {
      ElMessage.error(res.message || 'Error')
      
      // 401: 未授权
      if (response.status === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
      
      return Promise.reject(new Error(res.message || 'Error'))
    }
    
    return res
  },
  (error) => {
    console.error('Response error:', error)
    
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求地址不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(error.response.data?.message || error.message || '网络错误')
      }
    } else {
      ElMessage.error('网络错误，请检查后端服务')
    }
    
    return Promise.reject(error)
  }
)

export default service
