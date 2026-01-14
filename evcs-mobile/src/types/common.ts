/**
 * API 响应通用类型
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  success: boolean
  timestamp?: string
  traceId?: string
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 分页请求参数
 */
export interface PageParams {
  current?: number
  size?: number
}

/**
 * 坐标位置
 */
export interface Location {
  latitude: number
  longitude: number
  address?: string
}
