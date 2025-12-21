<template>
  <div class="charger-detail">
    <el-page-header @back="handleBack" content="充电桩详情">
      <template #extra>
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑配置
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
            <el-descriptions-item label="充电桩ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="充电桩编码">{{ detail.chargerCode }}</el-descriptions-item>
            <el-descriptions-item label="桩名称">{{ detail.chargerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属充电站">{{ detail.stationCode || detail.stationId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="充电桩类型">{{ getChargerTypeText(detail.chargerType) }}</el-descriptions-item>
            <el-descriptions-item label="额定功率">{{ detail.ratedPower ?? '-' }} kW</el-descriptions-item>
            <el-descriptions-item label="输入电压">{{ detail.inputVoltage ?? '-' }} V</el-descriptions-item>
            <el-descriptions-item label="输出电压范围">{{ detail.outputVoltageRange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="输出电流范围">{{ detail.outputCurrentRange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="枪头数量">{{ detail.gunCount ?? 1 }}</el-descriptions-item>
            <el-descriptions-item label="枪头类型">
              <el-tag v-for="t in parseGunTypes(detail.gunTypes)" :key="t" size="small" style="margin-right: 6px;">
                {{ t }}
              </el-tag>
              <span v-if="parseGunTypes(detail.gunTypes).length === 0">-</span>
            </el-descriptions-item>
            <el-descriptions-item label="支持协议">
              <el-tag v-for="protocol in parseSupportedProtocols(detail.supportedProtocols)" :key="protocol" size="small" style="margin-right: 5px;">
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
            <span>枪口信息</span>
          </template>

          <el-table :data="connectorTableRows" style="width: 100%">
            <el-table-column prop="connectorNo" label="枪口(ConnectorId)" width="160" />
            <el-table-column prop="connectorType" label="枪头类型" />
            <el-table-column label="状态" width="140">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="faultCode" label="故障码" width="160" />
            <el-table-column prop="faultDescription" label="故障描述" min-width="200" />
            <el-table-column prop="lastHeartbeat" label="最后心跳" width="200" />
            <el-table-column prop="currentSessionId" label="当前会话" width="180" />
            <el-table-column prop="currentUserId" label="当前用户" width="120" />
            <el-table-column label="开始时间" width="200">
              <template #default="scope">
                {{ formatDateTime(scope.row.chargingStartTime) }}
              </template>
            </el-table-column>
            <el-table-column label="已充电量(kWh)" width="140">
              <template #default="scope">
                {{ formatNumber(scope.row.chargedEnergy) }}
              </template>
            </el-table-column>
            <el-table-column label="已充电时长(分钟)" width="160">
              <template #default="scope">
                {{ formatNumber(scope.row.chargedDuration) }}
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="220">
              <template #default>
                <span style="color: #909399;">按 OCPP/云快充通常用桩编码 + 枪口号定位</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  type="success"
                  :disabled="!!scope.row.currentSessionId"
                  @click="handleStartConnector(scope.row)"
                >
                  启动
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  :disabled="!scope.row.currentSessionId"
                  @click="handleStopConnector(scope.row)"
                >
                  停止
                </el-button>
              </template>
            </el-table-column>
          </el-table>
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

  <el-dialog v-model="stopDialogVisible" :title="stopDialogTitle" width="420px" destroy-on-close>
    <el-form :model="stopDialogForm" label-width="120px">
      <el-form-item label="充电量(kWh)">
        <el-input v-model="stopDialogForm.energy" placeholder="例如 12.5" />
      </el-form-item>
      <el-form-item label="充电时长(分钟)">
        <el-input v-model="stopDialogForm.duration" placeholder="例如 30" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="stopDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="stopDialogSubmitting" @click="submitStopDialog">确定停止</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getChargerConnectors,
  getChargerDetail,
  startChargerConnectorSession,
  stopChargerConnectorSession
} from '@/api/charger'
import type { Charger, ChargerConnector } from '@/api/charger'

const router = useRouter()
const route = useRoute()

const loading = ref(false)

const stopDialogVisible = ref(false)
const stopDialogSubmitting = ref(false)
const stopDialogConnectorNo = ref<number | null>(null)
const stopDialogSessionId = ref<string | undefined>(undefined)
const stopDialogForm = ref({
  energy: '0',
  duration: '0'
})

const stopDialogTitle = computed(() => {
  return `停止充电(按枪口${stopDialogConnectorNo.value ?? '-'})`
})

const parseGunTypes = (gunTypes?: string) => {
  if (!gunTypes) return []
  return gunTypes
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
}

const buildGunList = (gunCount?: number, gunTypes?: string) => {
  const count = typeof gunCount === 'number' && gunCount > 0 ? gunCount : 1
  const types = parseGunTypes(gunTypes)
  return Array.from({ length: count }).map((_, idx) => {
    const connectorNo = idx + 1
    const gunType = types.length === count ? types[idx] : (types.length === 1 ? types[0] : (types.length > 0 ? types.join(', ') : undefined))
    return {
      id: 0,
      chargerId: detail.value.id,
      connectorNo,
      connectorType: gunType,
      status: detail.value.status,
      lastHeartbeat: detail.value.lastHeartbeat
    } as ChargerConnector
  })
}

const parseSupportedProtocols = (supportedProtocols?: string) => {
  if (!supportedProtocols) return []
  try {
    const parsed = JSON.parse(supportedProtocols)
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed).map(([k, v]) => `${k}${v ? ` ${String(v)}` : ''}`)
    }
    return []
  } catch {
    return []
  }
}

const formatDateTime = (v?: string) => {
  if (!v) return '-'
  return String(v)
}

const formatNumber = (v?: number | string) => {
  if (v === null || v === undefined) return '-'
  const n = Number(v)
  if (Number.isNaN(n)) return String(v)
  return String(n)
}

const getChargerTypeText = (chargerType?: number) => {
  const textMap: Record<number, string> = {
    1: '直流快充',
    2: '交流慢充'
  }
  if (typeof chargerType !== 'number') return '未知'
  return textMap[chargerType] || '未知'
}

const detail = ref<Charger>({
  id: 0,
  chargerCode: '',
  stationId: 0,
  chargerType: 1,
  status: 0
})

const connectors = ref<ChargerConnector[]>([])

const connectorTableRows = computed(() => {
  if (connectors.value.length > 0) {
    return connectors.value
  }
  return buildGunList(detail.value.gunCount, detail.value.gunTypes)
})

const realtimeData = ref({
  voltage: 0,
  current: 0,
  power: 0,
  soc: 0,
  energy: 0,
  duration: 0
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

const loadDetail = async () => {
  const chargerId = Number(route.params.id || route.query.id)
  if (!chargerId) {
    ElMessage.error('充电桩ID不能为空')
    return
  }
  loading.value = true
  try {
    const response = await getChargerDetail(chargerId)
    if (response.code === 200 && response.data) {
      detail.value = response.data as Charger
      try {
        const connectorResp = await getChargerConnectors(chargerId)
        if (connectorResp.code === 200 && Array.isArray(connectorResp.data)) {
          connectors.value = connectorResp.data
        } else {
          connectors.value = []
        }
      } catch (e) {
        connectors.value = []
      }
      realtimeData.value = {
        voltage: Number(detail.value.currentVoltage || 0),
        current: Number(detail.value.currentCurrent || 0),
        power: Number(detail.value.currentPower || 0),
        soc: 0,
        energy: Number(detail.value.chargedEnergy || 0),
        duration: Number(detail.value.chargedDuration || 0)
      }
    } else {
      ElMessage.error(response.message || '加载充电桩详情失败')
    }
  } catch (e) {
    console.error('加载充电桩详情失败:', e)
    ElMessage.error('加载充电桩详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'info',
    5: 'info',
    0: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '空闲',
    2: '充电中',
    3: '故障',
    4: '维护',
    5: '预约中',
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

const handleStartConnector = async (row: ChargerConnector) => {
  const chargerId = Number(detail.value.id)
  if (!chargerId) {
    ElMessage.error('充电桩ID不能为空')
    return
  }

  if (row.currentSessionId) {
    ElMessage.warning('该枪口已有进行中的会话')
    return
  }

  const currentUserId = Number(localStorage.getItem('userId') || '0')
  if (!currentUserId) {
    ElMessage.error('未获取到当前登录用户，请重新登录')
    return
  }

  try {
    const sessionId = `ADMIN_${Date.now()}`

    await startChargerConnectorSession(chargerId, row.connectorNo, {
      sessionId,
      userId: currentUserId,
      initialEnergy: 0
    })
    ElMessage.success(`已启动：枪口${row.connectorNo}，会话${sessionId}`)
    await loadDetail()
  } catch (e) {
    // cancel or request failed
  }
}

const handleStopConnector = async (row: ChargerConnector) => {
  const chargerId = Number(detail.value.id)
  if (!chargerId) {
    ElMessage.error('充电桩ID不能为空')
    return
  }

  if (!row.currentSessionId) {
    ElMessage.warning('该枪口当前没有进行中的会话')
    return
  }

  stopDialogConnectorNo.value = row.connectorNo
  stopDialogSessionId.value = row.currentSessionId
  stopDialogForm.value = { energy: '0', duration: '0' }
  stopDialogVisible.value = true
}

const submitStopDialog = async () => {
  const chargerId = Number(detail.value.id)
  if (!chargerId) {
    ElMessage.error('充电桩ID不能为空')
    return
  }

  const energy = Number(stopDialogForm.value.energy)
  const duration = Number(stopDialogForm.value.duration)
  if (!Number.isFinite(energy) || energy < 0) {
    ElMessage.error('充电量(kWh)必须为非负数字')
    return
  }
  if (!Number.isFinite(duration) || duration < 0 || !Number.isInteger(duration)) {
    ElMessage.error('充电时长(分钟)必须为非负整数')
    return
  }

  try {
    await ElMessageBox.confirm('确定要停止充电吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    stopDialogSubmitting.value = true
    const connectorNo = stopDialogConnectorNo.value
    if (!connectorNo) {
      ElMessage.error('枪口号不能为空')
      return
    }
    await stopChargerConnectorSession(chargerId, connectorNo, {
      sessionId: stopDialogSessionId.value,
      energy,
      duration
    })
    ElMessage.success(`已停止：枪口${connectorNo}`)

    stopDialogVisible.value = false
    await loadDetail()
  } catch (e) {
    // cancel or request failed
  } finally {
    stopDialogSubmitting.value = false
  }
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
