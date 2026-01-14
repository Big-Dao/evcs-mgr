import { get } from '@/utils/request'
import type { ApiResponse, PageResult, Station, Charger, StationSearchParams } from '@/types'

/**
 * 充电站 API
 * 
 * 后端服务: evcs-station (端口 8082)
 * 网关路由: /api/station/** -> evcs-station
 * 配置文件: config-repo/evcs-gateway-local.yml
 */
const STATION_API = '/api/station'

/**
 * 搜索充电站列表
 */
export function searchStations(params: StationSearchParams): Promise<ApiResponse<PageResult<Station>>> {
  return get(`${STATION_API}/search`, params as unknown as Record<string, unknown>)
}

/**
 * 获取附近充电站
 */
export function getNearbyStations(latitude: number, longitude: number, radius = 5000): Promise<ApiResponse<Station[]>> {
  return get(`${STATION_API}/nearby`, { latitude, longitude, radius })
}

/**
 * 获取充电站详情
 */
export function getStationDetail(id: number): Promise<ApiResponse<Station>> {
  return get(`${STATION_API}/${id}`)
}

/**
 * 获取充电站的充电桩列表
 */
export function getStationChargers(stationId: number): Promise<ApiResponse<Charger[]>> {
  return get(`${STATION_API}/${stationId}/chargers`)
}

/**
 * 获取充电桩详情
 */
export function getChargerDetail(chargerId: number): Promise<ApiResponse<Charger>> {
  return get(`/v1/chargers/${chargerId}`)
}

/**
 * 收藏充电站
 */
export function favoriteStation(stationId: number): Promise<ApiResponse<null>> {
  return get(`${STATION_API}/${stationId}/favorite`, {}, { method: 'POST' } as unknown as Record<string, unknown>)
}

/**
 * 取消收藏充电站
 */
export function unfavoriteStation(stationId: number): Promise<ApiResponse<null>> {
  return get(`${STATION_API}/${stationId}/unfavorite`, {}, { method: 'DELETE' } as unknown as Record<string, unknown>)
}

/**
 * 获取收藏的充电站列表
 */
export function getFavoriteStations(): Promise<ApiResponse<Station[]>> {
  return get(`${STATION_API}/favorites`)
}
