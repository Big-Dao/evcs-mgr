import request from '../utils/request'
import type { PageResult } from './types'

/**
 * 用户相关接口
 */

// 用户列表查询参数
export interface UserQueryParams {
  username?: string
  loginIdentifier?: string
  realName?: string
  status?: number
  current?: number
  size?: number
}

// 用户信息
export interface User {
  id: number
  username: string
  loginIdentifier?: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  status: number
  userType: number
  tenantId: number
  createTime?: string
  updateTime?: string
}

// 用户表单数据
export interface UserForm {
  username: string
  password?: string
  loginIdentifier?: string
  realName?: string
  phone?: string
  email?: string
  gender?: number
  status: number
  userType?: number
}

/**
 * 获取用户列表
 */
export function getUserList(params: UserQueryParams) {
  return request<PageResult<User>>({
    url: '/user/list',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id: number) {
  return request<User>({
    url: `/user/${id}`,
    method: 'get'
  })
}

/**
 * 新增用户
 */
export function createUser(data: UserForm) {
  return request<void>({
    url: '/user',
    method: 'post',
    data
  })
}

/**
 * 更新用户
 */
export function updateUser(id: number, data: UserForm) {
  return request<void>({
    url: `/user/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUser(id: number) {
  return request<void>({
    url: `/user/${id}`,
    method: 'delete'
  })
}

/**
 * 重置用户密码
 */
export function resetUserPassword(id: number, newPassword: string) {
  return request<void>({
    url: `/user/${id}/reset-password`,
    method: 'post',
    data: { newPassword }
  })
}

/**
 * 获取用户角色列表
 */
export function getUserRoles(userId: number) {
  return request<string[]>({
    url: `/user/${userId}/roles`,
    method: 'get'
  })
}

/**
 * 分配用户角色
 */
export function assignUserRoles(userId: number, roleIds: number[]) {
  return request<void>({
    url: `/user/${userId}/roles`,
    method: 'post',
    data: { roleIds }
  })
}
