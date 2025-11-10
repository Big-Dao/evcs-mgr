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
    // Propagate tenant/user context for backend multi-tenant filtering
    const tenantId = localStorage.getItem('tenantId')
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId
    }
    const userId = localStorage.getItem('userId')
    if (userId) {
      config.headers['X-User-Id'] = userId
    }
    console.log('API请求:', config.method?.toUpperCase(), config.url)
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
      const message = error.response.data?.message || error.message || '网络错误'
      
      switch (status) {
        case 401:
          console.error('401错误 - API调用失败:', error.config?.method?.toUpperCase(), error.config?.url)
          console.error('401错误 - 响应数据:', error.response.data)

          // 对于可能不存在的API（开发中的功能），不自动退出，而是显示友好错误
          const problematicApis = ['/dashboard/', '/tenant/', '/user/', '/station/', '/charger/', '/order/']
          const isProblematicApi = problematicApis.some(api => error.config?.url?.includes(api))

          if (isProblematicApi) {
            console.error('开发中API 401错误，不自动退出:', error.config?.url)
            ElMessage.warning('该功能正在开发中，暂不可用')
          } else {
            console.error('401错误 - 即将清除token并重定向')
            ElMessage.error('未授权，请重新登录')
            localStorage.removeItem('token')
            console.error('401错误 - token已清除，即将重定向到登录页')
            window.location.href = '/login'
          }
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求地址不存在')
          break
        case 500:
          // 显示后端返回的具体错误消息
          ElMessage.error(message)
          break
        default:
          ElMessage.error(message)
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
