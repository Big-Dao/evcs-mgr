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
    
    // Handle different response codes
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    
    return res
  },
  (error) => {
    console.error('Response error:', error)
    ElMessage.error(error.message || 'Network error')
    return Promise.reject(error)
  }
)

export default service
