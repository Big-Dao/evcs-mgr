import request from '../utils/request'
import type { PageResult } from './types'

/**
 * 租户相关接口
 */

// 租户列表查询参数
export interface TenantQueryParams {
  tenantName?: string
  tenantCode?: string
  tenantType?: number
  parentId?: number
  status?: number
  page?: number
  size?: number
}

// 租户信息
export interface Tenant {
  id: number
  tenantCode: string
  tenantName: string
  tenantType: number
  parentId?: number
  parentName?: string
  tenantTypeName?: string
  childrenCount?: number
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status: number
  maxUsers?: number
  maxStations?: number
  maxChargers?: number
  expireTime?: string
  remark?: string | null
  createTime?: string
  updateTime?: string
  children?: Tenant[]
}

// 租户表单数据
export interface TenantForm {
  tenantCode: string
  tenantName: string
  tenantType: number
  parentId?: number
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status: number
  maxUsers?: number | null
  maxStations?: number | null
  maxChargers?: number | null
  expireTime?: string | null
  remark?: string | null
}

/**
 * 获取租户列表
 */
export function getTenantList(params: TenantQueryParams) {
  return request<PageResult<Tenant>>({
    url: '/tenant/page',
    method: 'get',
    params
  })
}

/**
 * 获取租户详情
 */
export function getTenantDetail(id: number) {
  return request<Tenant>({
    url: `/tenant/${id}`,
    method: 'get'
  })
}

/**
 * 新增租户
 */
export function createTenant(data: TenantForm) {
  return request<void>({
    url: '/tenant',
    method: 'post',
    data
  })
}

/**
 * 更新租户
 */
export function updateTenant(id: number, data: TenantForm) {
  return request<void>({
    url: `/tenant/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除租户
 */
export function deleteTenant(id: number) {
  return request<void>({
    url: `/tenant/${id}`,
    method: 'delete'
  })
}

/**
 * 获取租户树形结构
 */
export function getTenantTree() {
  return request<Tenant[]>({
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
