import request from '../utils/request'

/**
 * 协议相关接口
 */

// 云快充请求
export interface CloudChargeRequest {
  deviceCode: string
  timestamp: string
  signature: string
  data?: any
}

// 云快充响应
export interface CloudChargeResponse {
  code: number
  message: string
  data?: any
  timestamp: string
}

// 协议事件
export interface ProtocolEvent {
  id: number
  eventType: string
  deviceCode: string
  protocol: string
  requestData: string
  responseData?: string
  status: string
  errorMessage?: string
  processTime: number
  tenantId: number
  createTime: string
}

// 协议事件查询参数
export interface ProtocolEventQueryParams {
  eventType?: string
  deviceCode?: string
  protocol?: string
  status?: string
  startTime?: string
  endTime?: string
  current?: number
  page?: number
  size?: number
}

// 协议统计
export interface ProtocolStatistics {
  totalEvents: number
  successEvents: number
  failedEvents: number
  successRate: number
  averageProcessTime: number
  eventsByType: Record<string, number>
  eventsByProtocol: Record<string, number>
}

// 设备协议状态
export interface DeviceProtocolStatus {
  deviceCode: string
  protocol: string
  online: boolean
  lastHeartbeat?: string
  connectionTime?: string
  disconnectionTime?: string
  messageCount: number
  errorCount: number
}

/**
 * 发送心跳
 */
export function sendHeartbeat(data: CloudChargeRequest) {
  return request<CloudChargeResponse>({
    url: '/cloudcharge/heartbeat',
    method: 'post',
    data
  })
}

/**
 * 上报状态
 */
export function reportStatus(data: CloudChargeRequest) {
  return request<CloudChargeResponse>({
    url: '/cloudcharge/status',
    method: 'post',
    data
  })
}

/**
 * 启动充电
 */
export function startCharging(data: CloudChargeRequest) {
  return request<CloudChargeResponse>({
    url: '/cloudcharge/start',
    method: 'post',
    data
  })
}

/**
 * 停止充电
 */
export function stopCharging(data: CloudChargeRequest) {
  return request<CloudChargeResponse>({
    url: '/cloudcharge/stop',
    method: 'post',
    data
  })
}

/**
 * 获取协议事件列表
 */
export function getProtocolEventList(params: ProtocolEventQueryParams) {
  const { current, page, ...rest } = params || {}

  return request<{ records: ProtocolEvent[]; total: number }>({
    url: '/protocol/events',
    method: 'get',
    params: {
      ...rest,
      page: page ?? current
    }
  })
}

/**
 * 获取协议事件详情
 */
export function getProtocolEventDetail(id: number) {
  return request<ProtocolEvent>({
    url: `/protocol/events/${id}`,
    method: 'get'
  })
}

/**
 * 获取协议统计
 */
export function getProtocolStatistics(startTime?: string, endTime?: string) {
  return request<ProtocolStatistics>({
    url: '/protocol/statistics',
    method: 'get',
    params: { startTime, endTime }
  })
}

/**
 * 获取设备协议状态
 */
export function getDeviceProtocolStatus(deviceCode: string) {
  return request<DeviceProtocolStatus>({
    url: `/protocol/device/${deviceCode}/status`,
    method: 'get'
  })
}

/**
 * 获取在线设备列表
 */
export function getOnlineDevices(protocol?: string) {
  return request<DeviceProtocolStatus[]>({
    url: '/protocol/devices/online',
    method: 'get',
    params: { protocol }
  })
}

/**
 * 调试接口 - 模拟协议消息
 */
export function debugProtocolMessage(data: {
  protocol: string
  messageType: string
  deviceCode: string
  payload: any
}) {
  return request<CloudChargeResponse>({
    url: '/debug/protocol/simulate',
    method: 'post',
    data
  })
}

/**
 * 调试接口 - 验证签名
 */
export function debugVerifySignature(data: {
  protocol: string
  requestData: string
  signature: string
}) {
  return request<{ valid: boolean; message: string }>({
    url: '/debug/protocol/verify-signature',
    method: 'post',
    data
  })
}

/**
 * 调试接口 - 解析协议消息
 */
export function debugParseMessage(data: {
  protocol: string
  message: string
}) {
  return request<{ parsed: any; errors?: string[] }>({
    url: '/debug/protocol/parse',
    method: 'post',
    data
  })
}
