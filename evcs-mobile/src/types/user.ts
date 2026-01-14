/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  phone: string
  avatar?: string
  tenantId?: number
  balance?: number
  createdAt?: string
}

/**
 * 登录请求参数
 */
export interface LoginRequest {
  phone: string
  code?: string
  password?: string
  loginType: 'sms' | 'password' | 'wechat' | 'alipay'
}

/**
 * 登录响应
 */
export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  expiresIn: number
  user: UserInfo
}

/**
 * 短信验证码请求
 */
export interface SmsCodeRequest {
  phone: string
  type: 'login' | 'register' | 'reset'
}
