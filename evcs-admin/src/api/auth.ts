import request from '../utils/request'

export interface LoginRequest {
  identifier: string
  password: string
}

export interface LoginUserInfo {
  id: number
  username: string
  identifier?: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  tenantId?: number
  tenantName?: string
  roles?: string[]
  permissions?: string[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  tokenType?: string
  expiresIn?: number
  user?: LoginUserInfo
}

/**
 * 用户登录
 */
export function login(data: LoginRequest) {
  return request<LoginResponse>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注销
 */
export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return request<LoginUserInfo>({
    url: '/auth/userinfo',
    method: 'get'
  })
}
