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
          <div class="chart-placeholder">
            <el-icon style="font-size: 48px; color: #dcdfe6;"><DataAnalysis /></el-icon>
            <p style="color: #909399; margin-top: 10px;">图表占位符</p>
          </div>
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
                ¥{{ row.amount.toFixed(2) }}
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
import { reactive, ref, onMounted } from 'vue'
import { type DashboardStats, type RecentOrder } from '@/api/dashboard'

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

// 加载统计数据
const loadStats = async () => {
  loading.value = true
  try {
    // 临时禁用API调用以避免401错误，直接使用mock数据
    // const response = await getDashboardStats()
    // if (response.data) {
    //   Object.assign(stats, response.data)
    // }
    console.log('Dashboard API暂时禁用，使用mock数据')
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
    // API不存在时使用mock数据
    stats.tenantCount = 12
    stats.stationCount = 48
    stats.chargerCount = 256
    stats.todayOrderCount = 89
  } finally {
    loading.value = false
  }
}

// 加载最近订单
const loadRecentOrders = async () => {
  try {
    // 临时禁用API调用以避免401错误，直接使用mock数据
    // const response = await getRecentOrders(5)
    // if (response.data) {
    //   recentOrders.value = response.data
    // }
    console.log('Recent orders API暂时禁用，使用mock数据')
  } catch (error: any) {
    console.error('加载最近订单失败:', error)
    // API不存在时使用mock数据
    recentOrders.value = [
      { orderId: 'ORD001', stationName: '市中心站', chargerCode: 'CH001', userName: '张三', amount: 45.80, status: '已完成', createTime: '2025-10-27 10:30:00' },
      { orderId: 'ORD002', stationName: '高新区站', chargerCode: 'CH002', userName: '李四', amount: 32.50, status: '充电中', createTime: '2025-10-27 11:15:00' },
      { orderId: 'ORD003', stationName: '机场站', chargerCode: 'CH003', userName: '王五', amount: 67.20, status: '已完成', createTime: '2025-10-27 12:00:00' }
    ]
  }
}

onMounted(() => {
  loadStats()
  loadRecentOrders()
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

.chart-placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #fafafa;
  border-radius: 4px;
}
</style>
