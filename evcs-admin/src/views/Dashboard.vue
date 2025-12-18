<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: #409eff;"><OfficeBuilding /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.tenantCount }}</div>
              <div class="stat-label">租户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: #67c23a;"><Location /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.stationCount }}</div>
              <div class="stat-label">充电站数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: #e6a23c;"><Monitor /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.chargerCount }}</div>
              <div class="stat-label">充电桩数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: #f56c6c;"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayOrderCount }}</div>
              <div class="stat-label">今日订单</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>充电桩状态分布</span>
          </template>
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近订单</span>
          </template>
          <el-table :data="recentOrders" style="width: 100%">
            <el-table-column prop="orderId" label="订单号" width="120" />
            <el-table-column prop="stationName" label="充电站" />
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">
                ¥{{ Number(row.amount ?? 0).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '已完成' ? 'success' : 'warning'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { type DashboardStats, type RecentOrder, getChargerStatusStats, getDashboardStats, getRecentOrders } from '@/api/dashboard'

const loading = ref(false)

const stats = reactive<DashboardStats>({
  tenantCount: 0,
  userCount: 0,
  stationCount: 0,
  chargerCount: 0,
  todayOrderCount: 0,
  todayChargingAmount: 0,
  todayRevenue: 0
})

const recentOrders = ref<RecentOrder[]>([])

// 图表实例与容器
let statusChart: echarts.ECharts | null = null
const statusChartRef = ref<HTMLDivElement | null>(null)

// 渲染状态分布图
const renderStatusChart = (data: { online: number; offline: number; charging: number; idle: number }) => {
  if (!statusChart && statusChartRef.value) {
    statusChart = echarts.init(statusChartRef.value)
    window.addEventListener('resize', resizeChart)
  }
  if (!statusChart) return

  const seriesData = [
    { name: '空闲', value: data.idle },
    { name: '充电中', value: data.charging },
    { name: '离线', value: data.offline },
    { name: '在线(其他)', value: Math.max(0, data.online - data.idle - data.charging) }
  ]

  statusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}: {c} ({d}%)' },
        data: seriesData
      }
    ]
  })
}

const resizeChart = () => statusChart?.resize()

// 加载统计数据
const loadStats = async () => {
  loading.value = true
  try {
    const response = await getDashboardStats()
    if (response.data) {
      Object.assign(stats, response.data)
    }
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载最近订单
const loadRecentOrders = async () => {
  try {
    const response = await getRecentOrders(5)
    if (response.data) {
      recentOrders.value = response.data
    }
  } catch (error: any) {
    console.error('加载最近订单失败:', error)
  }
}

onMounted(() => {
  loadStats()
  loadRecentOrders()
  getChargerStatusStats()
    .then(res => {
      if (res.data) {
        renderStatusChart(res.data as any)
      } else {
        renderStatusChart({ online: 200, offline: 10, charging: 30, idle: 60 })
      }
    })
    .catch(() => {
      renderStatusChart({ online: 200, offline: 10, charging: 30, idle: 60 })
    })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  statusChart?.dispose()
  statusChart = null
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 48px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

/* 状态分布图容器 */
.chart-box { height: 300px; }
</style>
