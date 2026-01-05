import request from '../utils/request'
import type { PageResult } from './types'

/**
 * 充电桩相关接口
 */

// 充电桩列表查询参数
export interface ChargerQueryParams {
  chargerCode?: string
  chargerName?: string
  stationId?: number
  status?: number
  chargerType?: number
  enabled?: number
  current?: number
  size?: number
}

// 充电桩信息
export interface Charger {
  id: number
  chargerCode: string
  chargerName?: string
  stationId: number
  stationCode?: string
  chargerType: number
  brand?: string
  model?: string
  manufacturer?: string
  productionDate?: string
  operationDate?: string
  ratedPower?: number
  inputVoltage?: number
  outputVoltageRange?: string
  outputCurrentRange?: string
  gunCount?: number
  gunTypes?: string
  supportedProtocols?: string
  status: number
  faultCode?: string
  faultDescription?: string
  lastHeartbeat?: string
  totalChargingSessions?: number
  totalChargingEnergy?: number
  totalChargingTime?: number
  currentSessionId?: string
  currentUserId?: number
  chargingStartTime?: string
  chargedEnergy?: number
  chargedDuration?: number
  currentPower?: number
  currentVoltage?: number
  currentCurrent?: number
  temperature?: number
  signalStrength?: number
  firmwareVersion?: string
  lastMaintenanceTime?: string
  nextMaintenanceTime?: string
  enabled?: number
  billingPlanId?: number
  remark?: string

  tenantId?: number
  createTime?: string
  updateTime?: string
}

// 充电枪口（Connector）信息
export interface ChargerConnector {
  id: number
  chargerId: number
  connectorNo: number
  connectorType?: string
  status: number
  faultCode?: string
  faultDescription?: string
  lastHeartbeat?: string
  currentSessionId?: string
  currentUserId?: number
  chargingStartTime?: string
  chargedEnergy?: number | string
  chargedDuration?: number

  // latest telemetry snapshot (per connector)
  lastMeterTime?: string
  lastVoltage?: number
  lastCurrent?: number
  lastPower?: number
  lastSoc?: number
  lastEnergy?: number

  tenantId?: number
  createTime?: string
  updateTime?: string
}

// 枪口会话历史（会话维度）
export interface ChargerConnectorSession {
  id: number
  chargerId: number
  connectorNo: number
  sessionId: string
  protocolType?: string
  startTime?: string
  stopTime?: string
  initialEnergy?: number
  totalEnergy?: number
  durationSeconds?: number
  lastSampleTime?: string
  lastVoltage?: number
  lastCurrent?: number
  lastPower?: number
  lastSoc?: number
  lastEnergy?: number
  status?: number

  tenantId?: number
  createTime?: string
  updateTime?: string
}

// 枪口会话曲线点（时间序列）
export interface ChargerConnectorCurvePoint {
  id: number
  chargerId: number
  connectorNo: number
  sessionId: string
  sampleTime: string
  voltage?: number
  current?: number
  power?: number
  soc?: number
  energy?: number
  durationSeconds?: number

  tenantId?: number
  createTime?: string
  updateTime?: string
}

// 充电桩表单数据
export interface ChargerForm {
  chargerCode: string
  chargerName?: string
  stationId: number
  chargerType: number
  brand?: string
  model?: string
  manufacturer?: string
  productionDate?: string
  operationDate?: string
  ratedPower?: number
  inputVoltage?: number
  outputVoltageRange?: string
  outputCurrentRange?: string
  gunCount?: number
  gunTypes?: string
  supportedProtocols?: string
  firmwareVersion?: string
  enabled?: number
  status: number
}

/**
 * 获取充电桩列表
 */
export function getChargerList(params: ChargerQueryParams) {
  return request<PageResult<Charger>>({
    url: '/charger/list',
    method: 'get',
    params
  })
}

/**
 * 获取充电桩详情
 */
export function getChargerDetail(id: number) {
  return request<Charger>({
    url: `/charger/${id}`,
    method: 'get'
  })
}

/**
 * 获取充电桩枪口列表（后端落库的真实枪口状态）
 */
export function getChargerConnectors(chargerId: number) {
  return request<ChargerConnector[]>({
    url: `/charger/${chargerId}/connectors`,
    method: 'get'
  })
}

/**
 * 分页查询所有枪口列表
 */
export function getConnectorList(params: {
  current?: number
  size?: number
  connectorNo?: string
  chargerCode?: string
  status?: number
}) {
  return request<PageResult<ChargerConnector>>({
    url: '/charger/connector-list',
    method: 'get',
    params
  })
}

/**
 * 查询枪口历史会话列表（按会话分页）
 */
export function getChargerConnectorSessions(
  chargerId: number,
  connectorNo: number,
  params: { current?: number; size?: number } = {}
) {
  return request<PageResult<ChargerConnectorSession>>({
    url: `/charger/${chargerId}/connectors/${connectorNo}/sessions`,
    method: 'get',
    params
  })
}

/**
 * 查询某会话的曲线点（支持时间范围与分页）
 */
export function getChargerConnectorSessionCurve(
  chargerId: number,
  connectorNo: number,
  sessionId: string,
  params: { from?: string; to?: string; current?: number; size?: number } = {}
) {
  return request<PageResult<ChargerConnectorCurvePoint>>({
    url: `/charger/${chargerId}/connectors/${connectorNo}/sessions/${encodeURIComponent(sessionId)}/curve`,
    method: 'get',
    params
  })
}

/**
 * 新增充电桩
 */
export function createCharger(data: ChargerForm) {
  return request<void>({
    url: '/charger',
    method: 'post',
    data
  })
}

/**
 * 更新充电桩
 */
export function updateCharger(id: number, data: ChargerForm) {
  return request<void>({
    url: `/charger`,
    method: 'put',
    data: {
      ...data,
      id
    }
  })
}

/**
 * 删除充电桩
 */
export function deleteCharger(id: number) {
  return request<void>({
    url: `/charger/${id}`,
    method: 'delete'
  })
}

/**
 * 获取充电桩实时状态
 */
export function getChargerStatus(id: number) {
  return request({
    url: `/charger/${id}`,
    method: 'get'
  })
}

/**
 * 按枪口启动充电（仅更新枪口会话字段，实际协议启动由协议服务负责）
 */
export function startChargerConnectorSession(
  chargerId: number,
  connectorNo: number,
  params: { sessionId?: string; userId: number; initialEnergy?: number }
) {
  return request<void>({
    url: `/charger/${chargerId}/connectors/${connectorNo}/start`,
    method: 'post',
    params
  })
}

/**
 * 按枪口停止充电（仅更新枪口会话字段，实际协议停止由协议服务负责）
 */
export function stopChargerConnectorSession(
  chargerId: number,
  connectorNo: number,
  params: { sessionId?: string; energy?: number; duration?: number }
) {
  return request<void>({
    url: `/charger/${chargerId}/connectors/${connectorNo}/stop`,
    method: 'post',
    params
  })
}

/**
 * 更新充电桩状态（0离线，1空闲，2充电中，3故障，4维护，5预约中）
 */
export function updateChargerStatus(id: number, status: number) {
  return request<void>({
    url: `/charger/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 更新充电桩实时数据
 */
export function updateChargerRealtime(
  id: number,
  data: { power?: number; voltage?: number; current?: number; temperature?: number }
) {
  return request<void>({
    url: `/charger/${id}/realtime`,
    method: 'put',
    params: data
  })
}

/**
 * 重置充电桩
 */
export function resetCharger(id: number) {
  return request<void>({
    url: `/charger/${id}/reset`,
    method: 'post'
  })
}

/**
 * 启用/停用充电桩（enabled=1启用，0停用）
 */
export function enableCharger(id: number, enabled: 0 | 1) {
  return request<void>({
    url: `/charger/${id}/enable`,
    method: 'put',
    params: { enabled }
  })
}
