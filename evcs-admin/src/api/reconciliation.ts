import request from '../utils/request'
import type { PageResult } from './types'

/**
 * 对账相关接口
 */

// 对账任务查询参数
export interface ReconciliationQueryParams {
  channel?: string
  status?: string
  startDate?: string
  endDate?: string
  current?: number
  page?: number
  size?: number
}

// 对账任务
export interface ReconciliationTask {
  id: number
  taskNo: string
  channel: string
  reconciliationDate: string
  status: string
  totalCount: number
  matchedCount: number
  unmatchedCount: number
  exceptionCount: number
  totalAmount: number
  matchedAmount: number
  unmatchedAmount: number
  startTime?: string
  endTime?: string
  duration?: number
  tenantId: number
  createTime: string
  updateTime?: string
}

// 对账任务详情
export interface ReconciliationTaskDetail extends ReconciliationTask {
  statementFileUrl?: string
  reportFileUrl?: string
  errorMessage?: string
  executedBy?: string
}

// 对账异常
export interface ReconciliationException {
  id: number
  taskId: number
  taskNo: string
  exceptionType: string
  tradeNo: string
  channelTradeNo?: string
  orderNo: string
  amount: number
  channelAmount?: number
  status: string
  channelStatus?: string
  exceptionDetail: string
  handleStatus: string
  handleResult?: string
  handleTime?: string
  handledBy?: string
  tenantId: number
  createTime: string
  updateTime?: string
}

// 对账异常查询参数
export interface ExceptionQueryParams {
  taskId?: number
  exceptionType?: string
  handleStatus?: string
  startDate?: string
  endDate?: string
  current?: number
  page?: number
  size?: number
}

// 对账统计
export interface ReconciliationStatistics {
  totalTasks: number
  successTasks: number
  failedTasks: number
  runningTasks: number
  totalExceptions: number
  unresolvedExceptions: number
  resolvedExceptions: number
  totalAmount: number
  matchedAmount: number
  unmatchedAmount: number
}

// 执行对账请求
export interface ExecuteReconciliationRequest {
  channel: string
  reconciliationDate: string
  statementFileUrl?: string
}

// 对账报告
export interface ReconciliationReport {
  taskNo: string
  channel: string
  reconciliationDate: string
  summary: ReconciliationSummary
  matchedRecords: ReconciliationRecord[]
  unmatchedRecords: ReconciliationRecord[]
  exceptions: ReconciliationException[]
}

export interface ReconciliationSummary {
  totalCount: number
  matchedCount: number
  unmatchedCount: number
  exceptionCount: number
  matchRate: number
  totalAmount: number
  matchedAmount: number
}

export interface ReconciliationRecord {
  tradeNo: string
  channelTradeNo?: string
  orderNo: string
  amount: number
  channelAmount?: number
  status: string
  channelStatus?: string
  payTime?: string
  channelPayTime?: string
  matched: boolean
}

// 处理异常请求
export interface HandleExceptionRequest {
  exceptionId: number
  handleResult: string
  remark?: string
}

/**
 * 获取对账任务列表
 */
export function getReconciliationTaskList(params: ReconciliationQueryParams) {
  const { current, page, ...rest } = params || {}

  return request<PageResult<ReconciliationTask>>({
    url: '/reconciliation/tasks',
    method: 'get',
    params: {
      ...rest,
      page: page ?? current
    }
  })
}

/**
 * 获取对账任务详情
 */
export function getReconciliationTaskDetail(taskId: number) {
  return request<ReconciliationTaskDetail>({
    url: `/reconciliation/tasks/${taskId}`,
    method: 'get'
  })
}

/**
 * 执行对账
 */
export function executeReconciliation(data: ExecuteReconciliationRequest) {
  return request<{ taskNo: string; message: string }>({
    url: '/reconciliation/execute',
    method: 'post',
    data
  })
}

/**
 * 执行每日对账
 */
export function executeDailyReconciliation(channel: string, date: string) {
  return request<{ taskNo: string; message: string }>({
    url: `/reconciliation/daily/${channel}`,
    method: 'post',
    params: { date }
  })
}

/**
 * 获取对账报告
 */
export function getReconciliationReport(taskNo: string) {
  return request<ReconciliationReport>({
    url: `/reconciliation/report/${taskNo}`,
    method: 'get'
  })
}

/**
 * 导出对账报告
 */
export function exportReconciliationReport(taskNo: string) {
  return request<Blob>({
    url: `/reconciliation/report/${taskNo}/export`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 获取对账异常列表
 */
export function getReconciliationExceptionList(params: ExceptionQueryParams) {
  const { current, page, ...rest } = params || {}

  return request<PageResult<ReconciliationException>>({
    url: '/reconciliation/exceptions',
    method: 'get',
    params: {
      ...rest,
      page: page ?? current
    }
  })
}

/**
 * 获取异常详情
 */
export function getExceptionDetail(exceptionId: number) {
  return request<ReconciliationException>({
    url: `/reconciliation/exceptions/${exceptionId}`,
    method: 'get'
  })
}

/**
 * 处理对账异常
 */
export function handleReconciliationException(data: HandleExceptionRequest) {
  return request<void>({
    url: '/reconciliation/exceptions/handle',
    method: 'post',
    data
  })
}

/**
 * 批量处理异常
 */
export function batchHandleExceptions(exceptionIds: number[], handleResult: string, remark?: string) {
  return request<void>({
    url: '/reconciliation/exceptions/batch-handle',
    method: 'post',
    data: { exceptionIds, handleResult, remark }
  })
}

/**
 * 获取对账统计
 */
export function getReconciliationStatistics(startDate?: string, endDate?: string) {
  return request<ReconciliationStatistics>({
    url: '/reconciliation/statistics',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 重试对账任务
 */
export function retryReconciliationTask(taskId: number) {
  return request<{ taskNo: string; message: string }>({
    url: `/reconciliation/tasks/${taskId}/retry`,
    method: 'post'
  })
}

/**
 * 取消对账任务
 */
export function cancelReconciliationTask(taskId: number, reason?: string) {
  return request<void>({
    url: `/reconciliation/tasks/${taskId}/cancel`,
    method: 'post',
    data: { reason }
  })
}
