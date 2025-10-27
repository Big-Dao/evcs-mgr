import request from '../utils/request'

/**
 * 订单相关接口
 */

// 订单列表查询参数
export interface OrderQueryParams {
  orderNo?: string
  userId?: number
  stationId?: number
  status?: string
  startTime?: string
  endTime?: string
  current?: number
  size?: number
}

// 订单信息
export interface Order {
  id: number
  orderNo: string
  userId: number
  userName?: string
  stationId: number
  stationName?: string
  chargerId: number
  chargerCode?: string
  startTime: string
  endTime?: string
  chargingDuration: number
  chargingAmount: number
  totalAmount: number
  status: string
  tenantId: number
  createTime: string
  updateTime?: string
}

// 订单详情
export interface OrderDetail extends Order {
  userPhone?: string
  stationAddress?: string
  powerUsed: number
  serviceFee: number
  parkingFee: number
  discount: number
  paymentMethod?: string
  paymentTime?: string
  transactionId?: string
}

// 订单统计
export interface OrderStatistics {
  totalOrders: number
  completedOrders: number
  chargingOrders: number
  canceledOrders: number
  totalRevenue: number
  totalPowerUsed: number
  averageOrderAmount: number
}

/**
 * 获取订单列表
 */
export function getOrderList(params: OrderQueryParams) {
  return request({
    url: '/order/list',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id: number) {
  return request({
    url: `/order/${id}`,
    method: 'get'
  })
}

/**
 * 取消订单
 */
export function cancelOrder(id: number, reason?: string) {
  return request({
    url: `/order/${id}/cancel`,
    method: 'post',
    data: { reason }
  })
}

/**
 * 获取订单统计
 */
export function getOrderStatistics(startDate?: string, endDate?: string) {
  return request({
    url: '/order/statistics',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 导出订单数据
 */
export function exportOrders(params: OrderQueryParams) {
  return request({
    url: '/order/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
