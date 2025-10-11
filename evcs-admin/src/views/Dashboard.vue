<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" style="color: #409eff;"><OfficeBuilding /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.tenants }}</div>
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
              <div class="stat-value">{{ stats.stations }}</div>
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
              <div class="stat-value">{{ stats.chargers }}</div>
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
              <div class="stat-value">{{ stats.orders }}</div>
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
import { reactive } from 'vue'

const stats = reactive({
  tenants: 12,
  stations: 48,
  chargers: 256,
  orders: 89
})

const recentOrders = reactive([
  { orderId: 'ORD001', stationName: '市中心站', amount: 45.80, status: '已完成' },
  { orderId: 'ORD002', stationName: '高新区站', amount: 32.50, status: '充电中' },
  { orderId: 'ORD003', stationName: '机场站', amount: 67.20, status: '已完成' },
  { orderId: 'ORD004', stationName: '火车站', amount: 28.90, status: '已完成' }
])
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
