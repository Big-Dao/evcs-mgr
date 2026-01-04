<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <!-- 左侧主要内容区域 -->
      <el-col :span="18">
        <!-- 设备状态 -->
        <el-card class="dashboard-card device-status-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title">设备状态</span>
            </div>
          </template>
          <div class="device-status-content">
            <div class="status-list">
              <div class="status-item unavailable">
                <div class="status-tag">不可用</div>
                <div class="status-metrics">
                  <div class="metric">
                    <span class="label">故障</span>
                    <span class="value">0</span>
                  </div>
                  <div class="metric">
                    <span class="label">离线</span>
                    <span class="value">{{ chargerStats.offline }}</span>
                  </div>
                  <div class="metric">
                    <span class="label">挂起</span>
                    <span class="value">0</span>
                  </div>
                </div>
              </div>
              <div class="status-item in-use">
                <div class="status-tag">使用中</div>
                <div class="status-metrics">
                  <div class="metric">
                    <span class="label">占用</span>
                    <span class="value">0</span>
                  </div>
                  <div class="metric">
                    <span class="label">充电中</span>
                    <span class="value">{{ chargerStats.charging }}</span>
                  </div>
                </div>
              </div>
              <div class="status-item idle">
                <div class="status-tag">空闲</div>
                <div class="status-metrics">
                  <div class="metric">
                    <span class="label">空闲</span>
                    <span class="value">{{ chargerStats.idle }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="status-chart">
              <div ref="deviceStatusChartRef" style="width: 100%; height: 160px;"></div>
            </div>
          </div>
        </el-card>

        <!-- 经营概览 -->
        <el-card class="dashboard-card operation-card" shadow="hover">
          <template #header>
            <div class="card-header-tabs">
              <div 
                class="tab-item" 
                :class="{ active: activeTab === 'overview' }"
                @click="activeTab = 'overview'"
              >经营概览</div>
              <div 
                class="tab-item" 
                :class="{ active: activeTab === 'device' }"
                @click="activeTab = 'device'"
              >设备概览</div>
              <div 
                class="tab-item" 
                :class="{ active: activeTab === 'charging' }"
                @click="activeTab = 'charging'"
              >充电分布</div>
            </div>
          </template>
          
          <div class="operation-metrics">
            <div class="metric-item">
              <div class="metric-title">今日充电量 (度)</div>
              <div class="metric-value">{{ stats.todayChargingAmount || '0.00' }}</div>
              <div class="metric-sub">
                昨日 {{ (stats.todayChargingAmount * 0.9).toFixed(2) }}
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-title">今日订单量 (单)</div>
              <div class="metric-value">{{ stats.todayOrderCount || '0' }}</div>
              <div class="metric-sub">
                昨日 {{ Math.floor(stats.todayOrderCount * 0.8) }}
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-title">今日订单总额 (元)</div>
              <div class="metric-value">{{ stats.todayRevenue || '0.00' }}</div>
              <div class="metric-sub">
                昨日 {{ (stats.todayRevenue * 0.85).toFixed(2) }}
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-title">今日服务费收入 (元)</div>
              <div class="metric-value">{{ (stats.todayRevenue * 0.3).toFixed(2) }}</div>
              <div class="metric-sub">
                昨日 {{ (stats.todayRevenue * 0.3 * 0.9).toFixed(2) }}
              </div>
            </div>
          </div>

          <div class="chart-filter">
            <el-radio-group v-model="timeRange" size="small">
              <el-radio-button label="7days">近7天</el-radio-button>
              <el-radio-button label="30days">近30天</el-radio-button>
              <el-radio-button label="12months">近12个月</el-radio-button>
            </el-radio-group>
            <span class="date-range">2025-12-28 ~ 2026-01-03</span>
          </div>

          <div class="trend-chart">
            <div ref="trendChartRef" style="width: 100%; height: 350px;"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧侧边栏 -->
      <el-col :span="6">
        <!-- 待办 -->
        <el-card class="dashboard-card right-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title">待办</span>
              <el-icon class="more-icon"><ArrowRight /></el-icon>
            </div>
          </template>
          <div class="empty-state">
            <el-icon class="empty-icon"><List /></el-icon>
            <div class="empty-text">待办都完成啦~</div>
          </div>
        </el-card>

        <!-- 重要消息 -->
        <el-card class="dashboard-card right-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title">重要消息</span>
              <el-icon class="more-icon"><ArrowRight /></el-icon>
            </div>
          </template>
          <div class="empty-state">
            <el-icon class="empty-icon"><Bell /></el-icon>
            <div class="empty-text">近3天无未读重要消息</div>
          </div>
        </el-card>

        <!-- 商家学院 -->
        <el-card class="dashboard-card right-card academy-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="title">商家学院</span>
              <el-icon class="more-icon"><ArrowRight /></el-icon>
            </div>
          </template>
          <div class="academy-banner">
            <div class="banner-content">
              <h3>铁林慧充使用指南</h3>
              <p>慧充经营参谋功能更新</p>
            </div>
            <el-icon class="banner-icon"><Reading /></el-icon>
          </div>
          <div class="academy-list">
            <div class="academy-item">
              <el-tag size="small" type="success" effect="plain">课程</el-tag>
              <span class="text">铁林充电后台基础功能介绍</span>
            </div>
            <div class="academy-item">
              <el-tag size="small" type="success" effect="plain">课程</el-tag>
              <span class="text">慧充新版账单对账流程</span>
            </div>
            <div class="academy-item">
              <el-tag size="small" type="success" effect="plain">课程</el-tag>
              <span class="text">如何开具及交付司机发票</span>
            </div>
            <div class="academy-item">
              <el-tag size="small" type="success" effect="plain">功能</el-tag>
              <span class="text">慧充后台功能更新</span>
            </div>
          </div>
        </el-card>

        <!-- 广告位 -->
        <el-card class="dashboard-card right-card ad-card" shadow="hover" :body-style="{ padding: '0' }">
          <div class="ad-content">
            <div class="ad-text">
              <h3>充电站融资好帮手</h3>
              <p>优质金融产品服务等你来选</p>
            </div>
            <el-icon class="ad-icon"><Money /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { ArrowRight, List, Bell, Reading, Money } from '@element-plus/icons-vue'
import { type DashboardStats, getChargerStatusStats, getDashboardStats } from '@/api/dashboard'

const activeTab = ref('overview')
const timeRange = ref('7days')

const stats = reactive<DashboardStats>({
  tenantCount: 0,
  userCount: 0,
  stationCount: 0,
  chargerCount: 0,
  todayOrderCount: 0,
  todayChargingAmount: 0,
  todayRevenue: 0
})

const chargerStats = reactive({
  online: 0,
  offline: 0,
  charging: 0,
  idle: 0
})

// Charts
let deviceStatusChart: echarts.ECharts | null = null
const deviceStatusChartRef = ref<HTMLDivElement | null>(null)

let trendChart: echarts.ECharts | null = null
const trendChartRef = ref<HTMLDivElement | null>(null)

const initDeviceStatusChart = () => {
  if (!deviceStatusChartRef.value) return
  deviceStatusChart = echarts.init(deviceStatusChartRef.value)
  
  const total = chargerStats.online + chargerStats.offline + chargerStats.charging + chargerStats.idle
  
  const option = {
    series: [
      {
        type: 'pie',
        radius: ['60%', '80%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'center',
          formatter: () => `{total|${total}}\n{text|全部}`,
          rich: {
            total: {
              fontSize: 20,
              fontWeight: 'bold',
              color: '#333'
            },
            text: {
              fontSize: 12,
              color: '#999',
              padding: [5, 0, 0, 0]
            }
          }
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: chargerStats.offline, name: '不可用', itemStyle: { color: '#F56C6C' } },
          { value: chargerStats.charging, name: '使用中', itemStyle: { color: '#409EFF' } },
          { value: chargerStats.idle, name: '空闲', itemStyle: { color: '#67C23A' } }
        ]
      }
    ]
  }
  deviceStatusChart.setOption(option)
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['充电量', '订单总额', '服务费'],
      bottom: 0,
      icon: 'circle'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['12.28', '12.29', '12.30', '12.31', '01.01', '01.02', '01.03'],
      axisLine: { show: false },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '度',
        position: 'left',
        splitLine: {
          lineStyle: { type: 'dashed' }
        }
      },
      {
        type: 'value',
        name: '元',
        position: 'right',
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '充电量',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: [1200, 1320, 1010, 1340, 900, 2300, 2100],
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.01)' }
          ])
        }
      },
      {
        name: '订单总额',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        showSymbol: false,
        data: [2200, 1820, 1910, 2340, 2900, 3300, 3100],
        itemStyle: { color: '#E6A23C' }
      },
      {
        name: '服务费',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        showSymbol: false,
        data: [600, 520, 410, 640, 800, 900, 850],
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
  trendChart.setOption(option)
}

const loadData = async () => {
  try {
    const [statsRes, chargerRes] = await Promise.all([
      getDashboardStats(),
      getChargerStatusStats()
    ])
    
    if (statsRes.data) Object.assign(stats, statsRes.data)
    if (chargerRes.data) Object.assign(chargerStats, chargerRes.data)
    
    initDeviceStatusChart()
    initTrendChart()
  } catch (e) {
    console.error(e)
  }
}

const resizeCharts = () => {
  deviceStatusChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  deviceStatusChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.dashboard-card {
  margin-bottom: 20px;
  border-radius: 8px;
  border: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

/* Device Status */
.device-status-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-list {
  display: flex;
  gap: 40px;
  flex: 1;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  width: fit-content;
}

.status-item.unavailable .status-tag { background: #FEF0F0; color: #F56C6C; }
.status-item.in-use .status-tag { background: #ECF5FF; color: #409EFF; }
.status-item.idle .status-tag { background: #F0F9EB; color: #67C23A; }

.status-metrics {
  display: flex;
  gap: 30px;
}

.metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.metric .label { font-size: 12px; color: #909399; }
.metric .value { font-size: 20px; font-weight: bold; color: #303133; }

.status-chart {
  width: 200px;
}

/* Operation Overview */
.card-header-tabs {
  display: flex;
  gap: 30px;
  font-size: 16px;
  font-weight: bold;
  color: #909399;
  cursor: pointer;
}

.tab-item.active {
  color: #303133;
  position: relative;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -15px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  background: #409EFF;
  border-radius: 2px;
}

.operation-metrics {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
}

.metric-item {
  flex: 1;
}

.metric-title { font-size: 14px; color: #606266; margin-bottom: 10px; }
.metric-value { font-size: 28px; font-weight: bold; color: #303133; margin-bottom: 5px; }
.metric-sub { font-size: 12px; color: #909399; }

.chart-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.date-range { font-size: 12px; color: #909399; }

/* Right Sidebar */
.more-icon { cursor: pointer; color: #909399; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  color: #C0C4CC;
}

.empty-icon { font-size: 48px; margin-bottom: 10px; opacity: 0.5; }
.empty-text { font-size: 12px; }

.academy-banner {
  background: linear-gradient(135deg, #E1F3D8 0%, #F0F9EB 100%);
  border-radius: 8px;
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.banner-content h3 { margin: 0 0 5px 0; font-size: 16px; color: #303133; }
.banner-content p { margin: 0; font-size: 12px; color: #67C23A; }
.banner-icon { font-size: 40px; color: #67C23A; opacity: 0.8; }

.academy-list { display: flex; flex-direction: column; gap: 12px; }
.academy-item { display: flex; gap: 8px; align-items: center; font-size: 13px; color: #606266; cursor: pointer; }
.academy-item:hover { color: #409EFF; }

.ad-content {
  background: linear-gradient(135deg, #E6F7FF 0%, #F0FBFF 100%);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ad-text h3 { margin: 0 0 5px 0; font-size: 16px; color: #409EFF; }
.ad-text p { margin: 0; font-size: 12px; color: #909399; }
.ad-icon { font-size: 40px; color: #409EFF; opacity: 0.8; }
</style>
