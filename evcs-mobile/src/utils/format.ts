/**
 * 格式化距离
 */
export function formatDistance(meters: number): string {
  if (meters < 1000) {
    return `${Math.round(meters)}m`
  }
  return `${(meters / 1000).toFixed(1)}km`
}

/**
 * 格式化价格
 */
export function formatPrice(price: number, unit = '元'): string {
  return `${price.toFixed(2)}${unit}`
}

/**
 * 格式化功率
 */
export function formatPower(power: number): string {
  return `${power}kW`
}

/**
 * 格式化电量
 */
export function formatEnergy(energy: number): string {
  return `${energy.toFixed(2)}kWh`
}

/**
 * 格式化时长（秒）
 */
export function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}秒`
  }
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (hours > 0) {
    return minutes > 0 ? `${hours}小时${minutes}分钟` : `${hours}小时`
  }
  
  return `${minutes}分钟`
}

/**
 * 格式化日期时间
 */
export function formatDateTime(dateStr: string, format = 'YYYY-MM-DD HH:mm'): string {
  const date = new Date(dateStr)
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化相对时间
 */
export function formatRelativeTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return formatDateTime(dateStr, 'MM-DD HH:mm')
}

/**
 * 格式化手机号（隐藏中间4位）
 */
export function formatPhone(phone: string): string {
  if (!phone || phone.length !== 11) return phone
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

/**
 * 格式化订单号（显示后8位）
 */
export function formatOrderNo(orderNo: string): string {
  if (!orderNo || orderNo.length <= 8) return orderNo
  return `...${orderNo.slice(-8)}`
}
