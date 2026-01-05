import request from '@/utils/request'

// 固件信息接口
export interface Firmware {
  id?: number
  firmwareVersion: string
  model: string
  url: string
  md5?: string
  description?: string
  createTime?: string
}

// 升级任务接口
export interface FirmwareUpgradeTask {
  id?: number
  firmwareId: number
  chargerId: number
  status: string
  retryCount: number
  startTime?: string
  finishTime?: string
  errorMessage?: string
  createTime?: string
}

// 上传固件
export function uploadFirmware(data: Firmware) {
  return request({
    url: '/station/firmware/upload',
    method: 'post',
    data
  })
}

// 固件列表
export function listFirmware(params: any) {
  return request({
    url: '/station/firmware/list',
    method: 'get',
    params
  })
}

// 创建升级任务
export function createUpgradeTask(firmwareId: number, chargerId: number) {
  return request({
    url: '/station/firmware/upgrade',
    method: 'post',
    params: { firmwareId, chargerId }
  })
}

// 重试升级任务
export function retryUpgradeTask(taskId: number) {
  return request({
    url: '/station/firmware/retry',
    method: 'post',
    params: { taskId }
  })
}

// 任务列表
export function listTasks(params: any) {
  return request({
    url: '/station/firmware/tasks',
    method: 'get',
    params
  })
}
