<template>
  <div class="station-map-analytics">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>充电站地图分析</span>
          <el-space>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
            />
            <el-button @click="handleRefresh" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </el-space>
        </div>
      </template>

      <!-- 数据概览 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总订单数</div>
            <div class="stat-value">{{ totalOrders.toLocaleString() }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">覆盖城市</div>
            <div class="stat-value">{{ totalCities }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总充电量(kWh)</div>
            <div class="stat-value">{{ totalEnergy.toFixed(2) }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总金额(元)</div>
            <div class="stat-value">{{ totalAmount.toFixed(2) }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 地图可视化 -->
      <div ref="mapChartRef" class="map-chart" v-loading="loading"></div>

      <!-- 城市排行榜 -->
      <el-card class="ranking-card" shadow="never">
        <template #header>
          <span>订单量排行 TOP 10</span>
        </template>
        <el-table :data="topCities" :show-header="true" size="small">
          <el-table-column type="index" label="排名" width="60" align="center" />
          <el-table-column prop="city" label="城市" />
          <el-table-column prop="orderCount" label="订单数" align="right">
            <template #default="{ row }">
              {{ row.orderCount.toLocaleString() }}
            </template>
          </el-table-column>
          <el-table-column prop="stationCount" label="充电站数" width="100" align="right" />
          <el-table-column prop="totalAmount" label="总金额(元)" align="right">
            <template #default="{ row }">
              {{ Number(row.totalAmount).toFixed(2) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import * as echarts from 'echarts'
import { getCityOrderStatistics, type CityOrderStatistics } from '@/api/order'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dateRange = ref<[string, string] | null>(null)
const cityStatistics = ref<CityOrderStatistics[]>([])

// 地图图表实例
let mapChart: echarts.ECharts | null = null
const mapChartRef = ref<HTMLDivElement | null>(null)

// 计算总览数据
const totalOrders = computed(() => 
  cityStatistics.value.reduce((sum, city) => sum + city.orderCount, 0)
)

const totalCities = computed(() => cityStatistics.value.length)

const totalEnergy = computed(() => 
  cityStatistics.value.reduce((sum, city) => sum + (city.totalEnergy || 0), 0)
)

const totalAmount = computed(() => 
  cityStatistics.value.reduce((sum, city) => sum + Number(city.totalAmount || 0), 0)
)

// TOP 10 城市
const topCities = computed(() => 
  [...cityStatistics.value]
    .sort((a, b) => b.orderCount - a.orderCount)
    .slice(0, 10)
)

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    let startTime: string | undefined
    let endTime: string | undefined
    
    if (dateRange.value) {
      startTime = dateRange.value[0] + 'T00:00:00'
      endTime = dateRange.value[1] + 'T23:59:59'
    }
    
    const response = await getCityOrderStatistics(startTime, endTime)
    if (response.data) {
      cityStatistics.value = response.data
      renderMap()
    } else {
      ElMessage.warning('未获取到数据')
    }
  } catch (error: any) {
    console.error('加载城市订单统计失败:', error)
    ElMessage.error('加载数据失败: ' + (error.message || '未知错误'))
    // 使用模拟数据用于演示
    cityStatistics.value = generateMockData()
    renderMap()
  } finally {
    loading.value = false
  }
}

// 生成模拟数据
const generateMockData = (): CityOrderStatistics[] => {
  const cities = [
    { province: '浙江省', city: '杭州市' },
    { province: '浙江省', city: '宁波市' },
    { province: '浙江省', city: '温州市' },
    { province: '江苏省', city: '南京市' },
    { province: '江苏省', city: '苏州市' },
    { province: '上海市', city: '上海市' },
    { province: '广东省', city: '广州市' },
    { province: '广东省', city: '深圳市' },
    { province: '北京市', city: '北京市' },
    { province: '四川省', city: '成都市' }
  ]
  
  return cities.map(({ province, city }) => ({
    province,
    city,
    orderCount: Math.floor(Math.random() * 1000) + 100,
    stationCount: Math.floor(Math.random() * 20) + 5,
    totalEnergy: Math.random() * 10000 + 1000,
    totalAmount: Math.random() * 50000 + 10000
  }))
}

// 渲染地图 - 使用柱状图和饼图可视化（不依赖地图GeoJSON）
const renderMap = () => {
  if (!mapChart && mapChartRef.value) {
    mapChart = echarts.init(mapChartRef.value)
    window.addEventListener('resize', resizeChart)
  }
  if (!mapChart) return

  // 按省份聚合数据
  const provinceMap = new Map<string, { orderCount: number; cities: Set<string>; stationCount: number }>()
  cityStatistics.value.forEach(city => {
    const province = city.province
    if (!provinceMap.has(province)) {
      provinceMap.set(province, { orderCount: 0, cities: new Set(), stationCount: 0 })
    }
    const pData = provinceMap.get(province)!
    pData.orderCount += city.orderCount
    pData.cities.add(city.city)
    pData.stationCount += city.stationCount
  })

  const provinces = Array.from(provinceMap.entries()).map(([name, data]) => ({
    name,
    orderCount: data.orderCount,
    cityCount: data.cities.size,
    stationCount: data.stationCount
  })).sort((a, b) => b.orderCount - a.orderCount).slice(0, 10)

  // 城市数据
  const cities = [...cityStatistics.value].sort((a, b) => b.orderCount - a.orderCount).slice(0, 15)

  const option: echarts.EChartsOption = {
    title: [
      {
        text: '省份订单量分布',
        left: '5%',
        top: '5%',
        textStyle: {
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      {
        text: '城市订单量排名',
        left: '55%',
        top: '5%',
        textStyle: {
          fontSize: 16,
          fontWeight: 'bold'
        }
      }
    ],
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.componentType === 'series') {
          if (params.seriesName === '省份订单量') {
            const data = params.data
            return `${params.name}<br/>
              订单数: ${data.orderCount.toLocaleString()}<br/>
              城市数: ${data.cityCount}<br/>
              充电站: ${data.stationCount}`
          } else {
            return `${params.name}<br/>订单数: ${params.value.toLocaleString()}`
          }
        }
        return params.name
      }
    },
    grid: [
      {
        left: '5%',
        right: '52%',
        top: '15%',
        bottom: '5%',
        containLabel: true
      },
      {
        left: '55%',
        right: '5%',
        top: '15%',
        bottom: '5%',
        containLabel: true
      }
    ],
    xAxis: [
      {
        type: 'value',
        gridIndex: 0,
        name: '订单数',
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 1000) {
              return (value / 1000).toFixed(1) + 'K'
            }
            return value.toString()
          }
        }
      },
      {
        type: 'value',
        gridIndex: 1,
        name: '订单数',
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 1000) {
              return (value / 1000).toFixed(1) + 'K'
            }
            return value.toString()
          }
        }
      }
    ],
    yAxis: [
      {
        type: 'category',
        gridIndex: 0,
        data: provinces.map(p => p.name.replace(/省|市|自治区|特别行政区/g, '')),
        axisLabel: {
          interval: 0,
          fontSize: 12
        }
      },
      {
        type: 'category',
        gridIndex: 1,
        data: cities.map(c => c.city.replace(/市$/, '')),
        axisLabel: {
          interval: 0,
          fontSize: 11
        }
      }
    ],
    series: [
      {
        name: '省份订单量',
        type: 'bar',
        xAxisIndex: 0,
        yAxisIndex: 0,
        data: provinces.map(p => ({
          value: p.orderCount,
          orderCount: p.orderCount,
          cityCount: p.cityCount,
          stationCount: p.stationCount,
          name: p.name
        })),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#4575b4' },
            { offset: 1, color: '#74add1' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => params.value.toLocaleString()
        }
      },
      {
        name: '城市订单量',
        type: 'bar',
        xAxisIndex: 1,
        yAxisIndex: 1,
        data: cities.map(c => c.orderCount),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#f56c6c' },
            { offset: 1, color: '#f5a76c' }
          ])
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => params.value.toLocaleString()
        }
      }
    ]
  }

  mapChart.setOption(option)
}

const resizeChart = () => {
  mapChart?.resize()
}

const handleDateRangeChange = () => {
  loadData()
}

const handleRefresh = () => {
  loadData()
}

onMounted(() => {
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  mapChart?.dispose()
  mapChart = null
})
</script>

<style scoped>
.station-map-analytics {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.map-chart {
  height: 600px;
  width: 100%;
  margin-bottom: 20px;
}

.ranking-card {
  margin-top: 20px;
}
</style>
