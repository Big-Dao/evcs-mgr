import request from '../utils/request'
import type { PageResult } from './types'

/**
 * 支付相关接口
 */

// 支付查询参数
export interface PaymentQueryParams {
  tradeNo?: string
  orderNo?: string
  channel?: string
  status?: string
  startTime?: string
  endTime?: string
  minAmount?: number
  maxAmount?: number
  current?: number
  page?: number
  size?: number
}

// 支付信息
export interface Payment {
  id: number
  tradeNo: string
  orderNo: string
  channel: string
  paymentMethod: string
  totalAmount: number
  paidAmount: number
  refundAmount: number
  status: string
  payTime?: string
  transactionId?: string
  userId: number
  userName?: string
  tenantId: number
  createTime: string
  updateTime?: string
}

// 支付详情
export interface PaymentDetail extends Payment {
  channelTradeNo?: string
  buyerId?: string
  buyerAccount?: string
  subject?: string
  body?: string
  notifyUrl?: string
  returnUrl?: string
  expireTime?: string
  attachInfo?: string
  errorCode?: string
  errorMsg?: string
}

// 支付统计
export interface PaymentStatistics {
  totalPayments: number
  successPayments: number
  failedPayments: number
  pendingPayments: number
  totalAmount: number
  successAmount: number
  refundAmount: number
  averageAmount: number
}

// 退款请求
export interface RefundRequest {
  tradeNo: string
  refundAmount: number
  refundReason: string
  notifyUrl?: string
}

// 退款响应
export interface RefundResponse {
  success: boolean
  refundNo: string
  refundAmount: number
  refundTime?: string
  message?: string
}

// 支付监控数据
export interface PaymentMonitoring {
  totalTransactions: number
  successRate: number
  averageResponseTime: number
  channelMetrics: ChannelMetrics[]
  realtimeMetrics: RealtimeMetrics
  performanceMetrics: PerformanceMetrics
}

// 渠道指标
export interface ChannelMetrics {
  channel: string
  totalCount: number
  successCount: number
  failCount: number
  successRate: number
  totalAmount: number
  averageResponseTime: number
}

// 实时指标
export interface RealtimeMetrics {
  currentTPS: number
  currentSuccessRate: number
  currentAverageResponseTime: number
  alertCount: number
}

// 性能指标
export interface PerformanceMetrics {
  p50ResponseTime: number
  p90ResponseTime: number
  p99ResponseTime: number
  maxResponseTime: number
  minResponseTime: number
}

// 健康检查结果
export interface HealthCheckResult {
  overall: string
  services: ServiceHealth[]
  timestamp: string
}

export interface ServiceHealth {
  name: string
  status: string
  responseTime: number
  message?: string
}

/**
 * 获取支付列表
 */
export function getPaymentList(params: PaymentQueryParams) {
  const { current, page, ...rest } = params || {}

  return request<PageResult<Payment>>({
    url: '/payment/list',
    method: 'get',
    params: {
      ...rest,
      page: page ?? current
    }
  })
}

/**
 * 查询支付详情
 */
export function queryPayment(tradeNo: string) {
  return request<PaymentDetail>({
    url: `/payment/query/${tradeNo}`,
    method: 'get'
  })
}

/**
 * 创建支付
 */
export function createPayment(data: {
  orderNo: string
  channel: string
  totalAmount: number
  subject: string
  body?: string
  notifyUrl?: string
  returnUrl?: string
}) {
  return request<{ tradeNo: string; qrCode?: string; payUrl?: string }>({
    url: '/payment/create',
    method: 'post',
    data
  })
}

/**
 * 申请退款
 */
export function refundPayment(data: RefundRequest) {
  return request<RefundResponse>({
    url: '/payment/refund',
    method: 'post',
    data
  })
}

/**
 * 获取支付统计
 */
export function getPaymentStatistics(startDate?: string, endDate?: string) {
  return request<PaymentStatistics>({
    url: '/payment/statistics',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取支付监控总览
 */
export function getPaymentMonitoringOverall(startTime?: string, endTime?: string) {
  return request<PaymentMonitoring>({
    url: '/payment/monitoring/overall',
    method: 'get',
    params: { startTime, endTime }
  })
}

/**
 * 获取指定渠道监控数据
 */
export function getChannelMonitoring(channel: string, startTime?: string, endTime?: string) {
  return request<ChannelMetrics>({
    url: `/payment/monitoring/channel/${channel}`,
    method: 'get',
    params: { startTime, endTime }
  })
}

/**
 * 获取时间范围内监控数据
 */
export function getTimeRangeMonitoring(startTime: string, endTime: string) {
  return request<PaymentMonitoring>({
    url: '/payment/monitoring/time-range',
    method: 'get',
    params: { startTime, endTime }
  })
}

/**
 * 获取健康检查结果
 */
export function getPaymentHealth() {
  return request<HealthCheckResult>({
    url: '/payment/monitoring/health',
    method: 'get'
  })
}

/**
 * 获取实时监控数据
 */
export function getRealtimeMonitoring() {
  return request<RealtimeMetrics>({
    url: '/payment/monitoring/realtime',
    method: 'get'
  })
}

/**
 * 获取性能指标
 */
export function getPerformanceMetrics(startTime?: string, endTime?: string) {
  return request<PerformanceMetrics>({
    url: '/payment/monitoring/performance',
    method: 'get',
    params: { startTime, endTime }
  })
}
