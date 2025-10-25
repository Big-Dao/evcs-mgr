import request from '../utils/request'

export interface LoginRequest {
  username: string
  password: string
  tenantId?: number
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  tenantId: number
}

/**
 * 用户登录
 */
export function login(data: LoginRequest) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注销
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return request({
    url: '/auth/user/info',
    method: 'get'
  })
}
