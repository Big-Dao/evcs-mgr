import { get, post } from '@/utils/request'
import type { ApiResponse, PaymentMethod } from '@/types'

/**
 * 支付 API
 * 
 * 后端服务: evcs-payment (端口 8084)
 * 网关路由: /api/payment/** -> evcs-payment
 * 配置文件: config-repo/evcs-gateway-local.yml
 */
const PAYMENT_API = '/api/payment'

/**
 * 钱包余额
 */
export interface WalletInfo {
  balance: number
  frozenAmount: number
  totalRecharge: number
  totalConsume: number
}

/**
 * 充值记录
 */
export interface RechargeRecord {
  id: number
  amount: number
  paymentMethod: PaymentMethod
  status: 'SUCCESS' | 'FAILED' | 'PENDING'
  createdAt: string
}

/**
 * 获取钱包信息
 */
export function getWalletInfo(): Promise<ApiResponse<WalletInfo>> {
  return get(`${PAYMENT_API}/wallet`)
}

/**
 * 获取充值记录
 */
export function getRechargeRecords(): Promise<ApiResponse<RechargeRecord[]>> {
  return get(`${PAYMENT_API}/wallet/records`)
}

/**
 * 创建充值订单
 */
export function createRechargeOrder(amount: number, paymentMethod: PaymentMethod): Promise<ApiResponse<{ orderId: string; payParams: Record<string, unknown> }>> {
  return post(`${PAYMENT_API}/recharge`, { amount, paymentMethod })
}

/**
 * 创建订单支付
 */
export function createOrderPayment(orderId: number, paymentMethod: PaymentMethod): Promise<ApiResponse<{ payParams: Record<string, unknown> }>> {
  return post(`${PAYMENT_API}/order/${orderId}/pay`, { paymentMethod })
}

/**
 * 查询支付结果
 */
export function queryPaymentResult(orderId: number): Promise<ApiResponse<{ status: 'SUCCESS' | 'FAILED' | 'PENDING' }>> {
  return get(`${PAYMENT_API}/order/${orderId}/status`)
}

/**
 * 微信支付调起
 */
export function invokeWechatPay(payParams: Record<string, unknown>): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.requestPayment({
      provider: 'wxpay',
      ...payParams,
      success: () => resolve(),
      fail: (err) => reject(err)
    })
    // #endif
    
    // #ifndef MP-WEIXIN
    reject(new Error('当前环境不支持微信支付'))
    // #endif
  })
}

/**
 * 支付宝支付调起
 */
export function invokeAlipay(payParams: Record<string, unknown>): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-ALIPAY
    uni.requestPayment({
      provider: 'alipay',
      orderInfo: payParams.orderInfo as string,
      success: () => resolve(),
      fail: (err) => reject(err)
    })
    // #endif
    
    // #ifndef MP-ALIPAY
    reject(new Error('当前环境不支持支付宝支付'))
    // #endif
  })
}
