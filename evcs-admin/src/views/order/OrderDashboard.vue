<template>
  <div class="order-dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日订单数" :value="stats.todayOrders">
            <template #suffix>笔</template>
            <template #prefix>
              <el-icon style="color: #409eff;"><Document /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-footer">
            <span :class="stats.ordersTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon><component :is="stats.ordersTrend > 0 ? 'Top' : 'Bottom'" /></el-icon>
              {{ Math.abs(stats.ordersTrend) }}%
            </span>
            <span style="color: #909399; font-size: 12px;">较昨日</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日营收" :value="stats.todayRevenue" :precision="2">
            <template #prefix>
              <el-icon style="color: #67c23a;"><Money /></el-icon>
              ¥
            </template>
          </el-statistic>
          <div class="stat-footer">
            <span :class="stats.revenueTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon><component :is="stats.revenueTrend > 0 ? 'Top' : 'Bottom'" /></el-icon>
              {{ Math.abs(stats.revenueTrend) }}%
            </span>
            <span style="color: #909399; font-size: 12px;">较昨日</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="今日充电量" :value="stats.todayEnergy" :precision="1">
            <template #suffix>kWh</template>
            <template #prefix>
              <el-icon style="color: #e6a23c;"><Lightning /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-footer">
            <span :class="stats.energyTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon><component :is="stats.energyTrend > 0 ? 'Top' : 'Bottom'" /></el-icon>
              {{ Math.abs(stats.energyTrend) }}%
            </span>
            <span style="color: #909399; font-size: 12px;">较昨日</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="充电中订单" :value="stats.chargingOrders">
            <template #suffix>笔</template>
            <template #prefix>
              <el-icon style="color: #f56c6c;"><Loading /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-footer">
            <span style="color: #909399; font-size: 12px;">实时数据</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>订单趋势</span>
              <el-radio-group v-model="trendPeriod" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">本年</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div style="height: 300px; background: #f5f5f5; display: flex; align-items: center; justify-content: center;">
            <div style="text-align: center; color: #999;">
              <el-icon :size="60"><TrendCharts /></el-icon>
              <p style="margin-top: 10px;">订单趋势图表（需集成 ECharts）</p>
              <p style="font-size: 14px;">显示订单数量和营收趋势</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>充电时段分布</span>
          </template>

          <div style="height: 300px; background: #f5f5f5; display: flex; align-items: center; justify-content: center;">
            <div style="text-align: center; color: #999;">
              <el-icon :size="60"><PieChart /></el-icon>
              <p style="margin-top: 10px;">时段分布饼图（需集成 ECharts）</p>
              <p style="font-size: 14px;">显示不同时段的订单占比</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>充电站订单排行</span>
          </template>

          <div class="ranking-list">
            <div v-for="(item, index) in stationRanking" :key="item.id" class="ranking-item">
              <div class="ranking-number" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
              <div class="ranking-content">
                <div class="ranking-name">{{ item.name }}</div>
                <el-progress :percentage="item.percentage" :show-text="false" />
              </div>
              <div class="ranking-value">{{ item.orders }}笔</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <span>充电桩使用率</span>
          </template>

          <div class="ranking-list">
            <div v-for="(item, index) in chargerUtilization" :key="item.id" class="ranking-item">
              <div class="ranking-number" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
              <div class="ranking-content">
                <div class="ranking-name">{{ item.code }}</div>
                <el-progress :percentage="item.utilization" :show-text="false" :color="getUtilizationColor(item.utilization)" />
              </div>
              <div class="ranking-value">{{ item.utilization }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <span>最近订单</span>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="order in recentOrders"
              :key="order.id"
              :timestamp="order.time"
              :type="order.type"
              size="small"
            >
              <div>
                <strong>{{ order.orderNo }}</strong>
                <el-tag :type="order.statusType" size="small" style="margin-left: 5px;">
                  {{ order.status }}
                </el-tag>
              </div>
              <div style="margin-top: 5px; font-size: 12px; color: #909399;">
                {{ order.station }} - {{ order.charger }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const trendPeriod = ref('week')

const stats = ref({
  todayOrders: 0,
  ordersTrend: 0,
  todayRevenue: 0,
  revenueTrend: 0,
  todayEnergy: 0,
  energyTrend: 0,
  chargingOrders: 0
})

// TODO: Backend API needed - GET /api/dashboard/station-ranking
const stationRanking = ref([
  { id: 1, name: '市中心充电站', orders: 45, percentage: 100 },
  { id: 2, name: '高新区充电站', orders: 38, percentage: 84 },
  { id: 3, name: '机场充电站', orders: 32, percentage: 71 },
  { id: 4, name: '商业区充电站', orders: 28, percentage: 62 },
  { id: 5, name: '工业园充电站', orders: 25, percentage: 56 }
])

// TODO: Backend API needed - GET /api/dashboard/charger-utilization
const chargerUtilization = ref([
  { id: 1, code: 'CH001', utilization: 92 },
  { id: 2, code: 'CH015', utilization: 88 },
  { id: 3, code: 'CH008', utilization: 85 },
  { id: 4, code: 'CH022', utilization: 78 },
  { id: 5, code: 'CH031', utilization: 72 }
])

const recentOrders = ref<any[]>([])

const loadDashboardData = async () => {
  try {
    const { getDashboardStats, getRecentOrders } = await import('@/api/dashboard')
    
    // Load dashboard statistics
    const statsResponse = await getDashboardStats()
    if (statsResponse.code === 200 && statsResponse.data) {
      const data = statsResponse.data
      stats.value = {
        todayOrders: data.todayOrderCount || 0,
        ordersTrend: 0, // TODO: Backend needs to provide trend data
        todayRevenue: data.todayRevenue || 0,
        revenueTrend: 0, // TODO: Backend needs to provide trend data
        todayEnergy: data.todayChargingAmount || 0,
        energyTrend: 0, // TODO: Backend needs to provide trend data
        chargingOrders: 0 // TODO: Backend needs to provide charging orders count
      }
    }
    
    // Load recent orders
    const ordersResponse = await getRecentOrders(10)
    if (ordersResponse.code === 200 && ordersResponse.data) {
      recentOrders.value = ordersResponse.data.map((order: any) => {
        // Map status to display format
        let statusType = 'info'
        let timelineType = 'primary'
        if (order.status === 'CHARGING') {
          statusType = 'warning'
          timelineType = 'primary'
        } else if (order.status === 'COMPLETED') {
          statusType = 'success'
          timelineType = 'success'
        }
        
        return {
          id: order.orderId,
          orderNo: order.orderId,
          time: order.createTime ? new Date(order.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : '',
          status: order.status,
          statusType,
          type: timelineType,
          station: order.stationName || '',
          charger: order.chargerCode || ''
        }
      })
    }
  } catch (error) {
    console.error('加载Dashboard数据失败:', error)
    ElMessage.error('加载Dashboard数据失败')
  }
}

onMounted(() => {
  loadDashboardData()
})

const getUtilizationColor = (utilization: number) => {
  if (utilization >= 80) return '#67c23a'
  if (utilization >= 60) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.order-dashboard {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.trend-up {
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
}

.trend-down {
  color: #f56c6c;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 14px;
}

.ranking-list {
  padding: 0;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-number {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 4px;
  font-weight: bold;
  color: #909399;
  font-size: 14px;
  flex-shrink: 0;
}

.ranking-number.rank-1 {
  background: linear-gradient(135deg, #ffd700, #ffa500);
  color: #fff;
}

.ranking-number.rank-2 {
  background: linear-gradient(135deg, #c0c0c0, #a8a8a8);
  color: #fff;
}

.ranking-number.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b8860b);
  color: #fff;
}

.ranking-content {
  flex: 1;
  min-width: 0;
}

.ranking-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ranking-value {
  font-size: 14px;
  color: #606266;
  font-weight: bold;
  flex-shrink: 0;
}
</style>
