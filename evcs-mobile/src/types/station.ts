/**
 * 充电站信息
 */
export interface Station {
  id: number
  name: string
  address: string
  latitude: number
  longitude: number
  distance?: number
  status: StationStatus
  openTime: string
  closeTime: string
  parkingFee?: number
  parkingFeeDesc?: string
  chargerCount: number
  availableCount: number
  fastChargerCount?: number
  slowChargerCount?: number
  images?: string[]
  phone?: string
  facilities?: string[]
  priceDesc?: string
  minPrice?: number
  maxPrice?: number
}

/**
 * 充电站状态
 */
export type StationStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE'

/**
 * 充电桩信息
 */
export interface Charger {
  id: number
  stationId: number
  code: string
  name: string
  type: ChargerType
  status: ChargerStatus
  power: number
  price: number
  connectors: Connector[]
}

/**
 * 充电桩类型
 */
export type ChargerType = 'DC' | 'AC'

/**
 * 充电桩状态
 */
export type ChargerStatus = 'AVAILABLE' | 'CHARGING' | 'OFFLINE' | 'FAULT'

/**
 * 充电枪/接口信息
 */
export interface Connector {
  id: number
  chargerId: number
  code: string
  type: ConnectorType
  status: ConnectorStatus
  power: number
}

/**
 * 接口类型
 */
export type ConnectorType = 'GB_DC' | 'GB_AC' | 'CCS1' | 'CCS2' | 'CHADEMO'

/**
 * 接口状态
 */
export type ConnectorStatus = 'AVAILABLE' | 'CHARGING' | 'OFFLINE' | 'FAULT' | 'RESERVED'

/**
 * 充电站搜索参数
 */
export interface StationSearchParams {
  keyword?: string
  latitude?: number
  longitude?: number
  radius?: number
  chargerType?: ChargerType
  status?: StationStatus
  current?: number
  size?: number
}
