import type { ApiResponse } from '@/types'

/**
 * 环境配置
 * 
 * 后端服务通过 evcs-gateway (端口 8080) 统一代理
 * 路由规则见: config-repo/evcs-gateway-local.yml
 * 
 * 路由映射:
 * - /api/auth/**     -> evcs-auth:8081
 * - /api/user/**     -> evcs-auth:8081
 * - /api/tenant/**   -> evcs-tenant:8086
 * - /api/station/**  -> evcs-station:8082
 * - /api/order/**    -> evcs-order:8083
 * - /api/payment/**  -> evcs-payment:8084
 */
const ENV_CONFIG = {
  development: {
    // 本地开发：直连 Gateway
    baseUrl: 'http://localhost:8080'
  },
  production: {
    // 生产环境：相对路径，由 nginx/小程序代理
    baseUrl: ''
  }
}

/**
 * 获取当前环境配置
 */
function getEnvConfig() {
  // #ifdef H5
  return ENV_CONFIG.development
  // #endif
  
  // #ifndef H5
  return ENV_CONFIG.production
  // #endif
}

const config = getEnvConfig()

/**
 * 请求配置
 */
interface RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  data?: Record<string, unknown>
  header?: Record<string, string>
  timeout?: number
  silent?: boolean
}

/**
 * 获取存储的 Token
 */
function getToken(): string {
  return uni.getStorageSync('token') || ''
}

/**
 * 获取存储的租户ID
 */
function getTenantId(): string {
  return uni.getStorageSync('tenantId') || ''
}

/**
 * 显示错误提示
 */
function showError(message: string) {
  uni.showToast({
    title: message,
    icon: 'none',
    duration: 2000
  })
}

/**
 * 处理登录过期
 */
function handleUnauthorized() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.removeStorageSync('tenantId')
  
  uni.showModal({
    title: '提示',
    content: '登录已过期，请重新登录',
    showCancel: false,
    success: () => {
      uni.reLaunch({
        url: '/pages/login/index'
      })
    }
  })
}

/**
 * 统一请求方法
 */
async function request<T = unknown>(options: RequestConfig): Promise<ApiResponse<T>> {
  const { url, method = 'GET', data, header = {}, timeout = 30000, silent = false } = options
  
  // 构建请求头
  const token = getToken()
  const tenantId = getTenantId()
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...header
  }
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  
  if (tenantId) {
    headers['X-Tenant-Id'] = tenantId
  }
  
  try {
    const response = await uni.request({
      url: `${config.baseUrl}${url}`,
      method,
      data,
      header: headers,
      timeout
    })
    
    const statusCode = response.statusCode
    const result = response.data as ApiResponse<T>
    
    // 处理 HTTP 状态码
    if (statusCode === 401) {
      handleUnauthorized()
      return Promise.reject(new Error('未授权'))
    }
    
    if (statusCode === 403) {
      if (!silent) showError('拒绝访问')
      return Promise.reject(new Error('拒绝访问'))
    }
    
    if (statusCode === 404) {
      if (!silent) showError('请求地址不存在')
      return Promise.reject(new Error('请求地址不存在'))
    }
    
    if (statusCode >= 500) {
      if (!silent) showError(result?.message || '服务器错误')
      return Promise.reject(new Error(result?.message || '服务器错误'))
    }
    
    // 处理业务状态码
    if (result.success === false || (result.code && result.code !== 200)) {
      if (!silent) showError(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    
    return result
  } catch (error: unknown) {
    const err = error as { errMsg?: string }
    console.error('Request error:', error)
    
    if (!silent) {
      if (err?.errMsg?.includes('timeout')) {
        showError('请求超时')
      } else if (err?.errMsg?.includes('fail')) {
        showError('网络连接失败')
      } else {
        showError('网络错误')
      }
    }
    
    return Promise.reject(error)
  }
}

/**
 * GET 请求
 */
export function get<T = unknown>(url: string, params?: Record<string, unknown>, options?: Partial<RequestConfig>): Promise<ApiResponse<T>> {
  // 拼接查询参数
  let queryUrl = url
  if (params && Object.keys(params).length > 0) {
    const queryString = Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && value !== '')
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
      .join('&')
    queryUrl = queryString ? `${url}?${queryString}` : url
  }
  
  return request<T>({
    url: queryUrl,
    method: 'GET',
    ...options
  })
}

/**
 * POST 请求
 */
export function post<T = unknown>(url: string, data?: Record<string, unknown>, options?: Partial<RequestConfig>): Promise<ApiResponse<T>> {
  return request<T>({
    url,
    method: 'POST',
    data,
    ...options
  })
}

/**
 * PUT 请求
 */
export function put<T = unknown>(url: string, data?: Record<string, unknown>, options?: Partial<RequestConfig>): Promise<ApiResponse<T>> {
  return request<T>({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

/**
 * DELETE 请求
 */
export function del<T = unknown>(url: string, data?: Record<string, unknown>, options?: Partial<RequestConfig>): Promise<ApiResponse<T>> {
  return request<T>({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

export default request
