import type { Location } from '@/types'

/**
 * 获取当前位置
 */
export function getCurrentLocation(): Promise<Location> {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      success: (res) => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude
        })
      },
      fail: (err) => {
        console.error('获取位置失败:', err)
        reject(err)
      }
    })
  })
}

/**
 * 打开地图导航
 */
export function openMapNavigation(location: Location, name: string) {
  uni.openLocation({
    latitude: location.latitude,
    longitude: location.longitude,
    name,
    address: location.address || '',
    scale: 18
  })
}

/**
 * 计算两点之间的距离（米）
 * 使用 Haversine 公式
 */
export function calculateDistance(
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number {
  const R = 6371000 // 地球半径（米）
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2)
  
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  
  return R * c
}

function toRad(deg: number): number {
  return deg * (Math.PI / 180)
}

/**
 * 检查定位权限
 */
export async function checkLocationPermission(): Promise<boolean> {
  // #ifdef MP-WEIXIN
  return new Promise((resolve) => {
    uni.getSetting({
      success: (res) => {
        if (res.authSetting['scope.userLocation'] === false) {
          uni.showModal({
            title: '位置权限',
            content: '需要获取您的位置信息来查找附近的充电站',
            confirmText: '去设置',
            success: (modalRes) => {
              if (modalRes.confirm) {
                uni.openSetting({
                  success: (settingRes) => {
                    resolve(!!settingRes.authSetting['scope.userLocation'])
                  },
                  fail: () => resolve(false)
                })
              } else {
                resolve(false)
              }
            }
          })
        } else {
          resolve(true)
        }
      },
      fail: () => resolve(false)
    })
  })
  // #endif
  
  // #ifndef MP-WEIXIN
  return true
  // #endif
}
