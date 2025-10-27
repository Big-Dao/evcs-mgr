import request from '../utils/request'

/**
 * 充电站相关接口
 */

// 充电站列表查询参数
export interface StationQueryParams {
  stationName?: string
  province?: string
  city?: string
  status?: number
  current?: number
  size?: number
}

// 充电站信息
export interface Station {
  id: number
  stationCode: string
  stationName: string
  province: string
  city: string
  district?: string
  address: string
  longitude?: number
  latitude?: number
  contactName?: string
  contactPhone?: string
  totalChargers: number
  availableChargers: number
  status: number
  tenantId: number
  createTime?: string
  updateTime?: string
}

// 充电站表单数据
export interface StationForm {
  stationCode: string
  stationName: string
  province: string
  city: string
  district?: string
  address: string
  longitude?: number
  latitude?: number
  contactName?: string
  contactPhone?: string
  status: number
}

/**
 * 获取充电站列表
 */
export function getStationList(params: StationQueryParams) {
  return request({
    url: '/station/list',
    method: 'get',
    params
  })
}

/**
 * 获取充电站详情
 */
export function getStationDetail(id: number) {
  return request({
    url: `/station/${id}`,
    method: 'get'
  })
}

/**
 * 新增充电站
 */
export function createStation(data: StationForm) {
  return request({
    url: '/station',
    method: 'post',
    data
  })
}

/**
 * 更新充电站
 */
export function updateStation(id: number, data: StationForm) {
  return request({
    url: `/station/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除充电站
 */
export function deleteStation(id: number) {
  return request({
    url: `/station/${id}`,
    method: 'delete'
  })
}

/**
 * 获取充电站下的充电桩列表
 */
export function getStationChargers(stationId: number) {
  return request({
    url: `/station/${stationId}/chargers`,
    method: 'get'
  })
}
