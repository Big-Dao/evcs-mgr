import request from '../utils/request'

/**
 * Dashboard 统计接口
 */

// Dashboard 统计数据
export interface DashboardStats {
  tenantCount: number
  userCount: number
  stationCount: number
  chargerCount: number
  todayOrderCount: number
  todayChargingAmount: number
  todayRevenue: number
}

// 充电桩状态统计
export interface ChargerStatusStats {
  online: number
  offline: number
  charging: number
  idle: number
}

// 最近订单
export interface RecentOrder {
  orderId: string
  stationName: string
  chargerCode: string
  userName: string
  amount: number
  status: string
  createTime: string
}

/**
 * 获取Dashboard统计数据
 */
export function getDashboardStats() {
  return request<DashboardStats>({
    url: '/dashboard/statistics',
    method: 'get'
  })
}

/**
 * 获取充电桩状态统计
 */
export function getChargerStatusStats() {
  return request<ChargerStatusStats>({
    url: '/dashboard/charger-status',
    method: 'get'
  })
}

/**
 * 获取最近订单列表
 */
export function getRecentOrders(limit: number = 10) {
  return request<RecentOrder[]>({
    url: '/dashboard/recent-orders',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取充电量趋势（按日期）
 */
export function getChargingTrend(days: number = 7) {
  return request<Array<{ date: string; value: number }>>({
    url: '/dashboard/charging-trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取收入趋势（按日期）
 */
export function getRevenueTrend(days: number = 7) {
  return request<Array<{ date: string; value: number }>>({
    url: '/dashboard/revenue-trend',
    method: 'get',
    params: { days }
  })
}
