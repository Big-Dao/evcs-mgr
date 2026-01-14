import { get, post } from '@/utils/request'
import type {
  ApiResponse,
  PageResult,
  Order,
  OrderQueryParams,
  StartChargingRequest,
  ChargingSession
} from '@/types'

/**
 * 订单 API
 * 
 * 后端服务: evcs-order (端口 8083)
 * 网关路由: /api/order/** -> evcs-order
 * 配置文件: config-repo/evcs-gateway-local.yml
 */
const ORDER_API = '/api/order'

/**
 * 获取订单列表
 */
export function getOrders(params?: OrderQueryParams): Promise<ApiResponse<PageResult<Order>>> {
  return get(`${ORDER_API}`, params as unknown as Record<string, unknown>)
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id: number): Promise<ApiResponse<Order>> {
  return get(`${ORDER_API}/${id}`)
}

/**
 * 根据订单号获取订单
 */
export function getOrderByNo(orderNo: string): Promise<ApiResponse<Order>> {
  return get(`${ORDER_API}/no/${orderNo}`)
}

/**
 * 启动充电
 */
export function startCharging(data: StartChargingRequest): Promise<ApiResponse<Order>> {
  return post(`${ORDER_API}/start`, data as unknown as Record<string, unknown>)
}

/**
 * 停止充电
 */
export function stopCharging(orderId: number): Promise<ApiResponse<Order>> {
  return post(`${ORDER_API}/${orderId}/stop`)
}

/**
 * 获取充电中的实时数据
 */
export function getChargingSession(orderId: number): Promise<ApiResponse<ChargingSession>> {
  return get(`${ORDER_API}/${orderId}/session`)
}

/**
 * 取消订单
 */
export function cancelOrder(orderId: number, reason?: string): Promise<ApiResponse<null>> {
  return post(`${ORDER_API}/${orderId}/cancel`, { reason })
}

/**
 * 获取进行中的订单
 */
export function getActiveOrder(): Promise<ApiResponse<Order | null>> {
  return get(`${ORDER_API}/active`)
}

/**
 * 订单评价
 */
export function rateOrder(orderId: number, rating: number, comment?: string): Promise<ApiResponse<null>> {
  return post(`${ORDER_API}/${orderId}/rate`, { rating, comment })
}
