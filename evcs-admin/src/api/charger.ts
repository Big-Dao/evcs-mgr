import request from '../utils/request'

/**
 * 充电桩相关接口
 */

// 充电桩列表查询参数
export interface ChargerQueryParams {
  chargerCode?: string
  stationId?: number
  status?: number
  current?: number
  size?: number
}

// 充电桩信息
export interface Charger {
  id: number
  chargerCode: string
  stationId: number
  stationName?: string
  chargerType: string
  power: number
  voltage: number
  current: number
  status: number
  connectorType: string
  price: number
  tenantId: number
  createTime?: string
  updateTime?: string
}

// 充电桩表单数据
export interface ChargerForm {
  chargerCode: string
  stationId: number
  chargerType: string
  power: number
  voltage: number
  current: number
  connectorType: string
  price: number
  status: number
}

/**
 * 获取充电桩列表
 */
export function getChargerList(params: ChargerQueryParams) {
  return request({
    url: '/charger/list',
    method: 'get',
    params
  })
}

/**
 * 获取充电桩详情
 */
export function getChargerDetail(id: number) {
  return request({
    url: `/charger/${id}`,
    method: 'get'
  })
}

/**
 * 新增充电桩
 */
export function createCharger(data: ChargerForm) {
  return request({
    url: '/charger',
    method: 'post',
    data
  })
}

/**
 * 更新充电桩
 */
export function updateCharger(id: number, data: ChargerForm) {
  return request({
    url: `/charger/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除充电桩
 */
export function deleteCharger(id: number) {
  return request({
    url: `/charger/${id}`,
    method: 'delete'
  })
}

/**
 * 获取充电桩实时状态
 */
export function getChargerStatus(id: number) {
  return request({
    url: `/charger/${id}/status`,
    method: 'get'
  })
}

/**
 * 远程控制充电桩（启动/停止）
 */
export function controlCharger(id: number, action: 'start' | 'stop') {
  return request({
    url: `/charger/${id}/control`,
    method: 'post',
    data: { action }
  })
}
