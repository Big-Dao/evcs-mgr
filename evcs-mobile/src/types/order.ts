/**
 * 充电订单
 */
export interface Order {
  id: number
  orderNo: string
  userId: number
  stationId: number
  stationName: string
  chargerId: number
  chargerCode: string
  connectorId: number
  connectorCode: string
  status: OrderStatus
  startTime?: string
  endTime?: string
  duration?: number
  energy?: number
  amount?: number
  serviceFee?: number
  totalAmount?: number
  paymentStatus: PaymentStatus
  paymentMethod?: PaymentMethod
  paymentTime?: string
  createdAt: string
}

/**
 * 订单状态
 */
export type OrderStatus = 
  | 'PENDING'      // 待充电
  | 'CHARGING'     // 充电中
  | 'COMPLETED'    // 已完成
  | 'CANCELLED'    // 已取消
  | 'FAILED'       // 充电失败

/**
 * 支付状态
 */
export type PaymentStatus = 
  | 'UNPAID'       // 未支付
  | 'PAYING'       // 支付中
  | 'PAID'         // 已支付
  | 'REFUNDING'    // 退款中
  | 'REFUNDED'     // 已退款

/**
 * 支付方式
 */
export type PaymentMethod = 'WECHAT' | 'ALIPAY' | 'BALANCE' | 'CARD'

/**
 * 充电会话实时数据
 */
export interface ChargingSession {
  orderId: number
  orderNo: string
  status: OrderStatus
  startTime: string
  duration: number
  energy: number
  power: number
  voltage: number
  current: number
  soc: number
  estimatedEndTime?: string
  estimatedEnergy?: number
  currentAmount: number
}

/**
 * 启动充电请求
 */
export interface StartChargingRequest {
  stationId: number
  chargerId: number
  connectorId: number
  paymentMethod?: PaymentMethod
}

/**
 * 订单查询参数
 */
export interface OrderQueryParams {
  status?: OrderStatus
  startDate?: string
  endDate?: string
  current?: number
  size?: number
}
