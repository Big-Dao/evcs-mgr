import { get, post } from '@/utils/request'
import type { ApiResponse, LoginRequest, LoginResponse, SmsCodeRequest, UserInfo } from '@/types'

/**
 * 认证 API
 * 
 * 后端服务: evcs-auth (端口 8081)
 * 网关路由: /api/auth/** -> evcs-auth
 * 配置文件: config-repo/evcs-gateway-local.yml
 */
const AUTH_API = '/api/auth'

/**
 * 用户 API
 * 
 * 后端服务: evcs-auth (端口 8081)
 * 网关路由: /api/user/** -> evcs-auth
 */
const USER_API = '/api/user'

/**
 * 发送短信验证码
 */
export function sendSmsCode(data: SmsCodeRequest): Promise<ApiResponse<null>> {
  return post(`${AUTH_API}/sms/send`, data as unknown as Record<string, unknown>)
}

/**
 * 手机号验证码登录
 */
export function loginByPhone(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  return post(`${AUTH_API}/mobile/login`, data as unknown as Record<string, unknown>)
}

/**
 * 微信小程序登录
 */
export function loginByWechat(code: string): Promise<ApiResponse<LoginResponse>> {
  return post(`${AUTH_API}/wechat/login`, { code })
}

/**
 * 支付宝小程序登录
 */
export function loginByAlipay(code: string): Promise<ApiResponse<LoginResponse>> {
  return post(`${AUTH_API}/alipay/login`, { code })
}

/**
 * 刷新 Token
 */
export function refreshToken(refreshToken: string): Promise<ApiResponse<LoginResponse>> {
  return post(`${AUTH_API}/refresh`, { refreshToken })
}

/**
 * 登出
 */
export function logout(): Promise<ApiResponse<null>> {
  return post(`${AUTH_API}/logout`)
}

/**
 * 获取当前用户信息
 */
export function getUserInfo(): Promise<ApiResponse<UserInfo>> {
  return get(`${USER_API}/me`)
}

/**
 * 更新用户信息
 */
export function updateUserInfo(data: Partial<UserInfo>): Promise<ApiResponse<UserInfo>> {
  return post(`${USER_API}/update`, data as unknown as Record<string, unknown>)
}

/**
 * 绑定手机号
 */
export function bindPhone(phone: string, code: string): Promise<ApiResponse<null>> {
  return post(`${USER_API}/bindPhone`, { phone, code })
}
