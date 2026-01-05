import request from '@/utils/request'

// 充电策略接口
export interface ChargingProfile {
  id?: number
  chargerId: number
  connectorId: number
  stackLevel: number
  purpose: string
  kind: string
  limitKw: number
  validFrom?: string
  validTo?: string
  description?: string
  createTime?: string
}

// 创建策略
export function createProfile(data: ChargingProfile) {
  return request({
    url: '/station/smart-charging/profile',
    method: 'post',
    data
  })
}

// 下发策略
export function applyProfile(id: number) {
  return request({
    url: `/station/smart-charging/profile/${id}/apply`,
    method: 'post'
  })
}

// 策略列表
export function listProfiles(params: any) {
  return request({
    url: '/station/smart-charging/profile/list',
    method: 'get',
    params
  })
}
