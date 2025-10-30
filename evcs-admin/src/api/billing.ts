import request from '../utils/request'
import type { PageResult } from './types'

/**
 * 计费方案相关接口
 */

// 计费方案查询参数
export interface BillingPlanQueryParams {
  stationId?: number
  current?: number
  size?: number
}

// 计费方案
export interface BillingPlan {
  id: number
  planCode?: string
  planName: string
  planType: string
  stationId?: number
  status: number
  isDefault?: number
  createTime?: string
  updateTime?: string
}

// 计费方案分段
export interface BillingPlanSegment {
  id?: number
  planId: number
  startTime: string
  endTime: string
  electricityPrice: number
  servicePrice: number
  peakType?: string
}

/**
 * 获取计费方案列表
 */
export function getBillingPlanList(params: BillingPlanQueryParams) {
  return request<BillingPlan[]>({
    url: '/billing/plans',
    method: 'get',
    params
  })
}

/**
 * 分页获取计费方案列表
 */
export function getBillingPlanPage(params: BillingPlanQueryParams) {
  return request<PageResult<BillingPlan>>({
    url: '/billing/plans/page',
    method: 'get',
    params
  })
}

/**
 * 获取计费方案详情
 */
export function getBillingPlanDetail(planId: number) {
  return request<BillingPlan>({
    url: `/billing/plans/${planId}`,
    method: 'get'
  })
}

/**
 * 获取计费方案分段
 */
export function getBillingPlanSegments(planId: number) {
  return request<BillingPlanSegment[]>({
    url: `/billing/plans/${planId}/segments`,
    method: 'get'
  })
}

/**
 * 创建计费方案
 */
export function createBillingPlan(data: BillingPlan) {
  return request<BillingPlan>({
    url: '/billing/plans',
    method: 'post',
    data
  })
}

/**
 * 更新计费方案
 */
export function updateBillingPlan(data: BillingPlan) {
  return request<boolean>({
    url: '/billing/plans',
    method: 'put',
    data
  })
}

/**
 * 删除计费方案
 */
export function deleteBillingPlan(planId: number) {
  return request<boolean>({
    url: `/billing/plans/${planId}`,
    method: 'delete'
  })
}

/**
 * 设置默认计费方案
 */
export function setDefaultBillingPlan(planId: number, stationId: number) {
  return request<boolean>({
    url: `/billing/plans/${planId}/default`,
    method: 'post',
    params: { stationId }
  })
}

/**
 * 保存计费方案分段
 */
export function saveBillingPlanSegments(planId: number, segments: BillingPlanSegment[], requireFullDay: boolean = false) {
  return request<boolean>({
    url: `/billing/plans/${planId}/segments`,
    method: 'post',
    params: { requireFullDay },
    data: segments
  })
}
