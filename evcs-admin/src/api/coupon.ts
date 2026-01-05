import request from '@/utils/request'

export interface Coupon {
  id?: number
  userId: number
  name: string
  type: number // 1: Amount Off, 2: Discount
  value: number
  minAmount: number
  startTime?: string
  endTime?: string
  status: number // 0: Unused, 1: Used, 2: Expired
}

export function issueCoupon(params: any) {
  return request({
    url: '/order/coupon/issue',
    method: 'post',
    params
  })
}

export function listMyCoupons(userId: number, orderAmount?: number) {
  return request({
    url: '/order/coupon/my',
    method: 'get',
    params: { userId, orderAmount }
  })
}
