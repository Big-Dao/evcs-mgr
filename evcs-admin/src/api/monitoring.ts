import request from '../utils/request'

/**
 * 系统监控相关接口
 */

// 服务健康状态
export interface ServiceHealth {
  serviceName: string
  status: 'UP' | 'DOWN' | 'UNKNOWN'
  instanceCount: number
  healthyInstances: number
  unhealthyInstances: number
  responseTime: number
  lastCheckTime: string
  details?: ServiceHealthDetail[]
}

export interface ServiceHealthDetail {
  instanceId: string
  host: string
  port: number
  status: 'UP' | 'DOWN'
  uptime: number
  memory: MemoryInfo
  threads: ThreadInfo
}

export interface MemoryInfo {
  used: number
  max: number
  usagePercent: number
}

export interface ThreadInfo {
  active: number
  peak: number
  daemon: number
}

// 系统指标
export interface SystemMetrics {
  cpu: CpuMetrics
  memory: MemoryMetrics
  disk: DiskMetrics
  network: NetworkMetrics
  jvm: JvmMetrics
}

export interface CpuMetrics {
  usage: number
  systemLoad: number
  cores: number
}

export interface MemoryMetrics {
  total: number
  used: number
  free: number
  usagePercent: number
}

export interface DiskMetrics {
  total: number
  used: number
  free: number
  usagePercent: number
}

export interface NetworkMetrics {
  bytesIn: number
  bytesOut: number
  packetsIn: number
  packetsOut: number
}

export interface JvmMetrics {
  heapUsed: number
  heapMax: number
  heapUsagePercent: number
  nonHeapUsed: number
  threadCount: number
  gcCount: number
  gcTime: number
}

// 性能指标
export interface PerformanceMetrics {
  endpoint: string
  method: string
  avgResponseTime: number
  minResponseTime: number
  maxResponseTime: number
  p50ResponseTime: number
  p90ResponseTime: number
  p99ResponseTime: number
  requestCount: number
  successCount: number
  errorCount: number
  successRate: number
  qps: number
}

// 业务指标
export interface BusinessMetrics {
  activeOrders: number
  dailyOrders: number
  activeChargers: number
  onlineChargers: number
  dailyRevenue: number
  activeUsers: number
  dailyNewUsers: number
}

// 告警信息
export interface Alert {
  id: number
  alertType: string
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  message: string
  source: string
  status: 'ACTIVE' | 'RESOLVED' | 'ACKNOWLEDGED'
  createTime: string
  updateTime?: string
  resolvedTime?: string
}

// 告警查询参数
export interface AlertQueryParams {
  alertType?: string
  severity?: string
  status?: string
  source?: string
  startTime?: string
  endTime?: string
  current?: number
  page?: number
  size?: number
}

// 服务版本信息（来自 evcs-monitoring 聚合接口 /monitoring/versions）
export interface ServiceVersion {
  serviceName: string
  instanceId?: string
  host?: string
  port?: number
  reachable: boolean
  error?: string
  buildVersion?: string
  buildTime?: string
  gitCommit?: string
  imageTag?: string
  registry?: string
}

/**
 * 获取系统总览
 */
export function getSystemOverview() {
  return request<{
    services: ServiceHealth[]
    metrics: SystemMetrics
    business: BusinessMetrics
    alerts: Alert[]
  }>({
    url: '/monitoring/overview',
    method: 'get'
  })
}

/**
 * 获取所有服务版本信息（best-effort）
 */
export function getServiceVersions() {
  return request<ServiceVersion[]>({
    url: '/monitoring/versions',
    method: 'get'
  })
}

/**
 * 获取所有服务健康状态
 */
export function getAllServicesHealth() {
  return request<ServiceHealth[]>({
    url: '/monitoring/services/health',
    method: 'get'
  })
}

/**
 * 获取指定服务健康状态
 */
export function getServiceHealth(serviceName: string) {
  return request<ServiceHealth>({
    url: `/monitoring/services/${serviceName}/health`,
    method: 'get'
  })
}

/**
 * 获取系统指标
 */
export function getSystemMetrics() {
  return request<SystemMetrics>({
    url: '/monitoring/metrics/system',
    method: 'get'
  })
}

/**
 * 获取性能指标
 */
export function getPerformanceMetrics(startTime?: string, endTime?: string) {
  return request<PerformanceMetrics[]>({
    url: '/monitoring/metrics/performance',
    method: 'get',
    params: { startTime, endTime }
  })
}

/**
 * 获取业务指标
 */
export function getBusinessMetrics() {
  return request<BusinessMetrics>({
    url: '/monitoring/metrics/business',
    method: 'get'
  })
}

/**
 * 获取告警列表
 */
export function getAlertList(params: AlertQueryParams) {
  const { current, page, ...rest } = params || {}

  return request<{ records: Alert[]; total: number }>({
    url: '/monitoring/alerts',
    method: 'get',
    params: {
      ...rest,
      page: page ?? current
    }
  })
}

/**
 * 获取告警详情
 */
export function getAlertDetail(id: number) {
  return request<Alert>({
    url: `/monitoring/alerts/${id}`,
    method: 'get'
  })
}

/**
 * 确认告警
 */
export function acknowledgeAlert(id: number) {
  return request<void>({
    url: `/monitoring/alerts/${id}/acknowledge`,
    method: 'post'
  })
}

/**
 * 解决告警
 */
export function resolveAlert(id: number, resolution: string) {
  return request<void>({
    url: `/monitoring/alerts/${id}/resolve`,
    method: 'post',
    data: { resolution }
  })
}

/**
 * 获取告警统计
 */
export function getAlertStatistics(startDate?: string, endDate?: string) {
  return request<{
    totalAlerts: number
    activeAlerts: number
    resolvedAlerts: number
    criticalAlerts: number
    highAlerts: number
    mediumAlerts: number
    lowAlerts: number
    alertsByType: Record<string, number>
    alertsBySeverity: Record<string, number>
  }>({
    url: '/monitoring/alerts/statistics',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取实时日志
 */
export function getRealTimeLogs(params: {
  serviceName?: string
  level?: string
  keyword?: string
  lines?: number
}) {
  return request<string[]>({
    url: '/monitoring/logs/realtime',
    method: 'get',
    params
  })
}

/**
 * 搜索日志
 */
export function searchLogs(params: {
  serviceName?: string
  level?: string
  keyword?: string
  startTime?: string
  endTime?: string
  current?: number
  size?: number
}) {
  return request<{ records: any[]; total: number }>({
    url: '/monitoring/logs/search',
    method: 'get',
    params
  })
}
