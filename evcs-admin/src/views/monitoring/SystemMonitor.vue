<template>
  <div class="system-monitor">
    <el-row :gutter="20">
      <!-- 系统概览卡片 -->
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background: #409eff;">
              <el-icon :size="30"><Monitor /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">活跃服务</div>
              <div class="metric-value">{{ overview.activeServices }}/{{ overview.totalServices }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background: #67c23a;">
              <el-icon :size="30"><TrendCharts /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">系统CPU</div>
              <div class="metric-value">{{ metrics.cpu?.usage?.toFixed(1) || 0 }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background: #e6a23c;">
              <el-icon :size="30"><PieChart /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">内存使用</div>
              <div class="metric-value">{{ metrics.memory?.usagePercent?.toFixed(1) || 0 }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="background: #f56c6c;">
              <el-icon :size="30"><Warning /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-label">活跃告警</div>
              <div class="metric-value">{{ overview.activeAlerts }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 服务健康状态 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>服务健康状态</span>
              <el-button size="small" @click="loadServicesHealth">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-table :data="services" v-loading="loading.services">
            <el-table-column prop="serviceName" label="服务名称" width="200" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="instanceCount" label="实例数" width="100" />
            <el-table-column prop="healthyInstances" label="健康实例" width="100">
              <template #default="{ row }">
                <span style="color: #67c23a;">{{ row.healthyInstances }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="unhealthyInstances" label="异常实例" width="100">
              <template #default="{ row }">
                <span v-if="row.unhealthyInstances > 0" style="color: #f56c6c;">
                  {{ row.unhealthyInstances }}
                </span>
                <span v-else>0</span>
              </template>
            </el-table-column>
            <el-table-column prop="responseTime" label="响应时间(ms)" width="120" />
            <el-table-column prop="lastCheckTime" label="最后检查" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 系统指标 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>系统指标</span>
          </template>

          <div class="system-metrics">
            <div class="metric-item">
              <div class="metric-label">CPU使用率</div>
              <el-progress 
                :percentage="metrics.cpu?.usage || 0" 
                :color="getProgressColor(metrics.cpu?.usage || 0)"
              />
            </div>

            <div class="metric-item">
              <div class="metric-label">内存使用率</div>
              <el-progress 
                :percentage="metrics.memory?.usagePercent || 0" 
                :color="getProgressColor(metrics.memory?.usagePercent || 0)"
              />
            </div>

            <div class="metric-item">
              <div class="metric-label">磁盘使用率</div>
              <el-progress 
                :percentage="metrics.disk?.usagePercent || 0" 
                :color="getProgressColor(metrics.disk?.usagePercent || 0)"
              />
            </div>

            <div class="metric-item">
              <div class="metric-label">JVM堆内存</div>
              <el-progress 
                :percentage="metrics.jvm?.heapUsagePercent || 0" 
                :color="getProgressColor(metrics.jvm?.heapUsagePercent || 0)"
              />
            </div>

            <el-divider />

            <div class="metric-detail">
              <div>CPU核心数: {{ metrics.cpu?.cores || 0 }}</div>
              <div>系统负载: {{ metrics.cpu?.systemLoad?.toFixed(2) || 0 }}</div>
              <div>JVM线程数: {{ metrics.jvm?.threadCount || 0 }}</div>
              <div>GC次数: {{ metrics.jvm?.gcCount || 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 业务指标 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>业务指标</span>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="活跃订单">{{ business.activeOrders }}</el-descriptions-item>
            <el-descriptions-item label="今日订单">{{ business.dailyOrders }}</el-descriptions-item>
            <el-descriptions-item label="在线充电桩">{{ business.onlineChargers }}</el-descriptions-item>
            <el-descriptions-item label="充电桩总数">{{ business.activeChargers }}</el-descriptions-item>
            <el-descriptions-item label="今日营收">
              ¥{{ business.dailyRevenue?.toFixed(2) || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="活跃用户">{{ business.activeUsers }}</el-descriptions-item>
            <el-descriptions-item label="今日新增用户">{{ business.dailyNewUsers }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 活跃告警 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>活跃告警</span>
              <el-button size="small" type="primary" @click="handleViewAllAlerts">
                查看全部
              </el-button>
            </div>
          </template>

          <el-table :data="alerts" v-loading="loading.alerts" max-height="300">
            <el-table-column prop="severity" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="getSeverityType(row.severity)" size="small">
                  {{ row.severity }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="source" label="来源" width="120" />
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleAcknowledgeAlert(row)">
                  确认
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Monitor, TrendCharts, PieChart, Warning, Refresh } from '@element-plus/icons-vue'
import { 
  getSystemOverview,
  getAllServicesHealth,
  acknowledgeAlert
} from '@/api/monitoring'
import type { ServiceHealth, SystemMetrics, BusinessMetrics, Alert } from '@/api/monitoring'

const router = useRouter()

const loading = reactive({
  services: false,
  alerts: false
})

const overview = reactive({
  totalServices: 0,
  activeServices: 0,
  activeAlerts: 0
})

const metrics = ref<SystemMetrics>({
  cpu: { usage: 0, systemLoad: 0, cores: 0 },
  memory: { total: 0, used: 0, free: 0, usagePercent: 0 },
  disk: { total: 0, used: 0, free: 0, usagePercent: 0 },
  network: { bytesIn: 0, bytesOut: 0, packetsIn: 0, packetsOut: 0 },
  jvm: { heapUsed: 0, heapMax: 0, heapUsagePercent: 0, nonHeapUsed: 0, threadCount: 0, gcCount: 0, gcTime: 0 }
})

const business = ref<BusinessMetrics>({
  activeOrders: 0,
  dailyOrders: 0,
  activeChargers: 0,
  onlineChargers: 0,
  dailyRevenue: 0,
  activeUsers: 0,
  dailyNewUsers: 0
})

const services = ref<ServiceHealth[]>([])
const alerts = ref<Alert[]>([])

let refreshTimer: NodeJS.Timeout | null = null

// 加载系统总览
const loadSystemOverview = async () => {
  try {
    const response = await getSystemOverview()
    if (response.data) {
      services.value = response.data.services || []
      metrics.value = response.data.metrics || metrics.value
      business.value = response.data.business || business.value
      alerts.value = (response.data.alerts || []).slice(0, 5) // 只显示前5条

      // 更新概览数据
      overview.totalServices = services.value.length
      overview.activeServices = services.value.filter(s => s.status === 'UP').length
      overview.activeAlerts = alerts.value.length
    }
  } catch (error) {
    console.error('加载系统总览失败:', error)
    ElMessage.warning('加载系统总览失败，显示模拟数据')
    // Fallback to mock data
    loadMockData()
  }
}

// 加载服务健康状态
const loadServicesHealth = async () => {
  loading.services = true
  try {
    const response = await getAllServicesHealth()
    if (response.data) {
      services.value = response.data
      overview.totalServices = services.value.length
      overview.activeServices = services.value.filter(s => s.status === 'UP').length
    }
  } catch (error) {
    console.error('加载服务健康状态失败:', error)
    ElMessage.error('加载服务健康状态失败')
  } finally {
    loading.services = false
  }
}

// 加载模拟数据
const loadMockData = () => {
  services.value = [
    {
      serviceName: 'evcs-gateway',
      status: 'UP',
      instanceCount: 1,
      healthyInstances: 1,
      unhealthyInstances: 0,
      responseTime: 15,
      lastCheckTime: '2024-12-18 11:20:00'
    },
    {
      serviceName: 'evcs-station',
      status: 'UP',
      instanceCount: 1,
      healthyInstances: 1,
      unhealthyInstances: 0,
      responseTime: 20,
      lastCheckTime: '2024-12-18 11:20:00'
    },
    {
      serviceName: 'evcs-order',
      status: 'UP',
      instanceCount: 1,
      healthyInstances: 1,
      unhealthyInstances: 0,
      responseTime: 18,
      lastCheckTime: '2024-12-18 11:20:00'
    },
    {
      serviceName: 'evcs-payment',
      status: 'UP',
      instanceCount: 1,
      healthyInstances: 1,
      unhealthyInstances: 0,
      responseTime: 25,
      lastCheckTime: '2024-12-18 11:20:00'
    }
  ]

  metrics.value = {
    cpu: { usage: 35.5, systemLoad: 1.2, cores: 8 },
    memory: { total: 16384, used: 8192, free: 8192, usagePercent: 50.0 },
    disk: { total: 512000, used: 256000, free: 256000, usagePercent: 50.0 },
    network: { bytesIn: 1024000, bytesOut: 2048000, packetsIn: 5000, packetsOut: 5500 },
    jvm: { heapUsed: 1024, heapMax: 2048, heapUsagePercent: 50.0, nonHeapUsed: 512, threadCount: 150, gcCount: 100, gcTime: 1500 }
  }

  business.value = {
    activeOrders: 15,
    dailyOrders: 120,
    activeChargers: 50,
    onlineChargers: 48,
    dailyRevenue: 12560.50,
    activeUsers: 320,
    dailyNewUsers: 25
  }

  alerts.value = [
    {
      id: 1,
      alertType: 'SERVICE_DOWN',
      severity: 'HIGH',
      title: '服务响应时间过长',
      message: 'evcs-payment服务平均响应时间超过500ms',
      source: 'evcs-payment',
      status: 'ACTIVE',
      createTime: '2024-12-18 10:30:00'
    }
  ]

  overview.totalServices = services.value.length
  overview.activeServices = services.value.filter(s => s.status === 'UP').length
  overview.activeAlerts = alerts.value.length
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    UP: 'success',
    DOWN: 'danger',
    UNKNOWN: 'warning'
  }
  return typeMap[status] || 'info'
}

const getSeverityType = (severity: string) => {
  const typeMap: Record<string, string> = {
    CRITICAL: 'danger',
    HIGH: 'warning',
    MEDIUM: 'primary',
    LOW: 'info'
  }
  return typeMap[severity] || 'info'
}

const getProgressColor = (percentage: number) => {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#67c23a'
}

const handleViewAllAlerts = () => {
  router.push('/monitoring/alerts')
}

const handleAcknowledgeAlert = async (alert: Alert) => {
  try {
    await acknowledgeAlert(alert.id)
    ElMessage.success('已确认告警')
    loadSystemOverview()
  } catch (error) {
    console.error('确认告警失败:', error)
    ElMessage.error('确认告警失败')
  }
}

// 启动自动刷新
const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    loadSystemOverview()
  }, 30000) // 每30秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadSystemOverview()
  startAutoRefresh()
})

// 页面卸载时停止自动刷新
onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.system-monitor {
  padding: 20px;
}

.metric-card {
  height: 100%;
}

.metric-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.metric-info {
  flex: 1;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.system-metrics {
  padding: 10px 0;
}

.metric-item {
  margin-bottom: 20px;
}

.metric-item .metric-label {
  margin-bottom: 8px;
  color: #606266;
}

.metric-detail {
  font-size: 14px;
  color: #606266;
}

.metric-detail > div {
  margin-bottom: 8px;
}
</style>
