<template>
  <div class="charger-detail">
    <el-page-header @back="handleBack" content="充电桩详情">
      <template #extra>
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑配置
        </el-button>
        <el-button type="success" @click="handleStartCharge">
          <el-icon><VideoPlay /></el-icon>
          启动充电
        </el-button>
        <el-button type="danger" @click="handleStopCharge">
          <el-icon><VideoPause /></el-icon>
          停止充电
        </el-button>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-tag :type="getStatusType(detail.status)" size="large">
                <el-icon><component :is="getStatusIcon(detail.status)" /></el-icon>
                {{ getStatusText(detail.status) }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="充电桩ID">{{ detail.chargerId }}</el-descriptions-item>
            <el-descriptions-item label="充电桩编码">{{ detail.chargerCode }}</el-descriptions-item>
            <el-descriptions-item label="所属充电站">{{ detail.stationName }}</el-descriptions-item>
            <el-descriptions-item label="充电桩类型">{{ detail.chargerType }}</el-descriptions-item>
            <el-descriptions-item label="额定功率">{{ detail.power }} kW</el-descriptions-item>
            <el-descriptions-item label="额定电压">{{ detail.ratedVoltage }} V</el-descriptions-item>
            <el-descriptions-item label="额定电流">{{ detail.ratedCurrent }} A</el-descriptions-item>
            <el-descriptions-item label="充电接口">{{ detail.connectorType }}</el-descriptions-item>
            <el-descriptions-item label="支持协议">
              <el-tag v-for="protocol in detail.supportedProtocols" :key="protocol" size="small" style="margin-right: 5px;">
                {{ protocol }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="固件版本">{{ detail.firmwareVersion }}</el-descriptions-item>
            <el-descriptions-item label="最后心跳">{{ detail.lastHeartbeat }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="detail-card" style="margin-top: 20px;">
          <template #header>
            <span>实时数据</span>
          </template>

          <el-row :gutter="20">
            <el-col :span="8">
              <div class="realtime-metric">
                <div class="metric-label">当前电压</div>
                <div class="metric-value">{{ realtimeData.voltage }} <span class="metric-unit">V</span></div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="realtime-metric">
                <div class="metric-label">当前电流</div>
                <div class="metric-value">{{ realtimeData.current }} <span class="metric-unit">A</span></div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="realtime-metric">
                <div class="metric-label">当前功率</div>
                <div class="metric-value">{{ realtimeData.power }} <span class="metric-unit">kW</span></div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="8">
              <div class="realtime-metric">
                <div class="metric-label">SOC</div>
                <div class="metric-value">{{ realtimeData.soc }} <span class="metric-unit">%</span></div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="realtime-metric">
                <div class="metric-label">已充电量</div>
                <div class="metric-value">{{ realtimeData.energy }} <span class="metric-unit">kWh</span></div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="realtime-metric">
                <div class="metric-label">充电时长</div>
                <div class="metric-value">{{ realtimeData.duration }} <span class="metric-unit">分钟</span></div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card class="detail-card" style="margin-top: 20px;">
          <template #header>
            <span>历史数据趋势</span>
          </template>

          <div style="height: 300px; background: #f5f5f5; display: flex; align-items: center; justify-content: center;">
            <div style="text-align: center; color: #999;">
              <el-icon :size="60"><TrendCharts /></el-icon>
              <p style="margin-top: 10px;">图表组件占位（需集成 ECharts）</p>
              <p style="font-size: 14px;">显示功率、电流、电压等历史趋势</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <span>设备状态</span>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="event in statusEvents"
              :key="event.id"
              :timestamp="event.timestamp"
              :type="event.type"
            >
              {{ event.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>告警信息</span>
          </template>

          <el-empty v-if="alarms.length === 0" description="暂无告警" :image-size="100" />
          <div v-else>
            <el-alert
              v-for="alarm in alarms"
              :key="alarm.id"
              :title="alarm.title"
              :type="alarm.type"
              :description="alarm.description"
              style="margin-bottom: 10px;"
              show-icon
            />
          </div>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>今日统计</span>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="充电次数">{{ todayStats.chargeCount }} 次</el-descriptions-item>
            <el-descriptions-item label="充电电量">{{ todayStats.totalEnergy }} kWh</el-descriptions-item>
            <el-descriptions-item label="充电时长">{{ todayStats.totalDuration }} 小时</el-descriptions-item>
            <el-descriptions-item label="营收金额">¥{{ todayStats.revenue }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const detail = ref({
  chargerId: 1,
  chargerCode: 'CH001',
  stationName: '市中心充电站',
  chargerType: 'DC快充',
  power: 120,
  ratedVoltage: 380,
  ratedCurrent: 250,
  connectorType: 'GB/T 27930-2015',
  supportedProtocols: ['OCPP 1.6', 'CloudCharge'],
  firmwareVersion: 'v2.3.1',
  lastHeartbeat: '2024-10-12 10:30:00',
  createTime: '2024-01-15 14:20:00',
  status: 2
})

const realtimeData = ref({
  voltage: 375.5,
  current: 185.2,
  power: 69.5,
  soc: 68,
  energy: 32.5,
  duration: 28
})

const statusEvents = ref([
  {
    id: 1,
    timestamp: '2024-10-12 10:30:00',
    type: 'success',
    content: '充电中 - 用户订单 ORD20241012001'
  },
  {
    id: 2,
    timestamp: '2024-10-12 10:00:00',
    type: 'primary',
    content: '充电启动'
  },
  {
    id: 3,
    timestamp: '2024-10-12 09:45:00',
    type: 'info',
    content: '设备已连接'
  },
  {
    id: 4,
    timestamp: '2024-10-12 08:30:00',
    type: 'success',
    content: '充电完成 - 订单 ORD20241012000'
  }
])

const alarms = ref<any[]>([])

const todayStats = ref({
  chargeCount: 8,
  totalEnergy: 456.8,
  totalDuration: 12.5,
  revenue: 684.50
})

const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
    0: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '空闲',
    2: '充电中',
    3: '故障',
    0: '离线'
  }
  return textMap[status] || '未知'
}

const getStatusIcon = (status: number) => {
  const iconMap: Record<number, string> = {
    1: 'CircleCheck',
    2: 'Loading',
    3: 'CircleClose',
    0: 'WarningFilled'
  }
  return iconMap[status] || 'QuestionFilled'
}

const handleBack = () => {
  router.back()
}

const handleEdit = () => {
  ElMessage.info('编辑充电桩配置功能')
}

const handleStartCharge = () => {
  ElMessageBox.confirm('确定要启动充电吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('充电已启动')
  })
}

const handleStopCharge = () => {
  ElMessageBox.confirm('确定要停止充电吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('充电已停止')
  })
}
</script>

<style scoped>
.charger-detail {
  height: 100%;
}

.detail-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.realtime-metric {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.metric-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.metric-unit {
  font-size: 16px;
  font-weight: normal;
  color: #909399;
  margin-left: 5px;
}
</style>
