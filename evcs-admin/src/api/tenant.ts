import request from '../utils/request'

/**
 * 租户相关接口
 */

// 租户列表查询参数
export interface TenantQueryParams {
  name?: string
  type?: string
  status?: number
  current?: number
  size?: number
}

// 租户信息
export interface Tenant {
  id: number
  tenantCode: string
  tenantName: string
  tenantType: string
  parentId?: number
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status: number
  createTime?: string
  updateTime?: string
}

// 租户表单数据
export interface TenantForm {
  tenantCode: string
  tenantName: string
  tenantType: string
  parentId?: number
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status: number
}

/**
 * 获取租户列表
 */
export function getTenantList(params: TenantQueryParams) {
  return request({
    url: '/tenant/list',
    method: 'get',
    params
  })
}

/**
 * 获取租户详情
 */
export function getTenantDetail(id: number) {
  return request({
    url: `/tenant/${id}`,
    method: 'get'
  })
}

/**
 * 新增租户
 */
export function createTenant(data: TenantForm) {
  return request({
    url: '/tenant',
    method: 'post',
    data
  })
}

/**
 * 更新租户
 */
export function updateTenant(id: number, data: TenantForm) {
  return request({
    url: `/tenant/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除租户
 */
export function deleteTenant(id: number) {
  return request({
    url: `/tenant/${id}`,
    method: 'delete'
  })
}

/**
 * 获取租户树形结构
 */
export function getTenantTree() {
  return request({
    url: '/tenant/tree',
    method: 'get'
  })
}

/**
 * 获取租户统计信息
 */
export function getTenantStatistics() {
  return request({
    url: '/tenant/statistics',
    method: 'get'
  })
}
