import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T = unknown> {
  code?: number
  message?: string
  data?: T
  success?: boolean
  timestamp?: string
  traceId?: string
}

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
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 后端返回格式: { code: number, success?: boolean, message?: string, data: any }
    if (res.success === false || (res.success === undefined && res.code && res.code !== 200)) {
      ElMessage.error(res.message || 'Error')

      // 401: 未授权
      if (response.status === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message || 'Error'))
    }

    return response
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

const request = async <T = unknown>(config: AxiosRequestConfig): Promise<ApiResponse<T>> => {
  const response = await service.request<ApiResponse<T>>(config)
  return response.data
}

export default request
export { service }
