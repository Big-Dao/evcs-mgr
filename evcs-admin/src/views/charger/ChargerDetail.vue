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
                <el-button
                  size="small"
                  type="primary"
                  @click="openSessionDrawer(scope.row)"
                >
                  历史
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

  <el-drawer
    v-model="sessionDrawerVisible"
    :title="sessionDrawerTitle"
    size="80%"
    destroy-on-close
  >
    <div style="display: flex; gap: 16px; height: 100%;">
      <div style="flex: 1; min-width: 520px;">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px;">
              <span>会话列表</span>
              <div style="display: flex; align-items: center; gap: 10px;">
                <el-button size="small" @click="reloadSessions">刷新</el-button>
              </div>
            </div>
          </template>

          <el-table
            :data="sessionTableRows"
            style="width: 100%"
            height="520"
            v-loading="sessionDrawerLoading"
            @row-click="handleSessionRowClick"
          >
            <el-table-column prop="sessionId" label="会话ID" min-width="220" />
            <el-table-column prop="protocolType" label="协议" width="110" />
            <el-table-column label="开始时间" width="190">
              <template #default="scope">
                {{ formatDateTime(scope.row.startTime) }}
              </template>
            </el-table-column>
            <el-table-column label="结束时间" width="190">
              <template #default="scope">
                {{ formatDateTime(scope.row.stopTime) }}
              </template>
            </el-table-column>
            <el-table-column label="电量(kWh)" width="120">
              <template #default="scope">
                {{ formatNumber(scope.row.totalEnergy) }}
              </template>
            </el-table-column>
            <el-table-column label="时长(s)" width="100">
              <template #default="scope">
                {{ formatNumber(scope.row.durationSeconds) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button size="small" type="primary" @click.stop="selectSession(scope.row.sessionId)">
                  曲线
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div style="display: flex; justify-content: flex-end; margin-top: 12px;">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :total="sessionTotal"
              :page-size="sessionPageSize"
              :current-page="sessionCurrentPage"
              @current-change="(p:number)=>{ sessionCurrentPage = p; reloadSessions(); }"
              @size-change="(s:number)=>{ sessionPageSize = s; sessionCurrentPage = 1; reloadSessions(); }"
            />
          </div>
        </el-card>
      </div>

      <div style="flex: 1; min-width: 520px;">
        <el-card shadow="never" style="height: 100%;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px;">
              <span>会话曲线</span>
              <div style="display: flex; align-items: center; gap: 10px;">
                <el-date-picker
                  v-model="curveTimeRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="起始时间"
                  end-placeholder="结束时间"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 360px;"
                />
                <el-button
                  size="small"
                  type="primary"
                  :disabled="!selectedSessionId"
                  :loading="curveLoading"
                  @click="reloadCurve"
                >
                  加载
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="!selectedSessionId" style="height: 560px; display: flex; align-items: center; justify-content: center; color: #909399;">
            请选择一个会话查看曲线
          </div>
          <div v-else style="height: 560px;" v-loading="curveLoading">
            <div ref="curveChartRef" style="width: 100%; height: 100%;" />
          </div>
        </el-card>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  getChargerConnectors,
  getChargerDetail,
  getChargerConnectorSessions,
  getChargerConnectorSessionCurve,
  startChargerConnectorSession,
  stopChargerConnectorSession
} from '@/api/charger'
import type {
  Charger,
  ChargerConnector,
  ChargerConnectorCurvePoint,
  ChargerConnectorSession
} from '@/api/charger'

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
      status: -1,
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

// 历史会话/曲线 drawer
const sessionDrawerVisible = ref(false)
const sessionDrawerLoading = ref(false)
const sessionDrawerConnectorNo = ref<number | null>(null)
const sessionCurrentPage = ref(1)
const sessionPageSize = ref(10)
const sessionTotal = ref(0)
const sessionTableRows = ref<ChargerConnectorSession[]>([])
const selectedSessionId = ref<string | null>(null)

const curveLoading = ref(false)
const curveTimeRange = ref<[string, string] | null>(null)
const curvePoints = ref<ChargerConnectorCurvePoint[]>([])
const curveChartRef = ref<HTMLDivElement | null>(null)
const curveChart = shallowRef<echarts.ECharts | null>(null)

const sessionDrawerTitle = computed(() => {
  const cid = detail.value?.id ? String(detail.value.id) : '-'
  const cno = sessionDrawerConnectorNo.value ?? '-'
  return `历史会话/曲线（充电桩${cid} - 枪口${cno}）`
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

const getStatusType = (status?: number) => {
  if (typeof status !== 'number' || status < 0) return 'info'
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

const getStatusText = (status?: number) => {
  if (typeof status !== 'number' || status < 0) return '未知'
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

const openSessionDrawer = async (row: ChargerConnector) => {
  sessionDrawerConnectorNo.value = row.connectorNo
  sessionCurrentPage.value = 1
  sessionPageSize.value = 10
  selectedSessionId.value = null
  curveTimeRange.value = null
  curvePoints.value = []
  sessionDrawerVisible.value = true

  await reloadSessions()
}

const reloadSessions = async () => {
  const chargerId = Number(detail.value.id)
  const connectorNo = sessionDrawerConnectorNo.value
  if (!chargerId || !connectorNo) return
  sessionDrawerLoading.value = true
  try {
    const resp = await getChargerConnectorSessions(chargerId, connectorNo, {
      current: sessionCurrentPage.value,
      size: sessionPageSize.value
    })
    if (resp.code === 200 && resp.data) {
      sessionTableRows.value = resp.data.records || []
      sessionTotal.value = resp.data.total || 0
    } else {
      sessionTableRows.value = []
      sessionTotal.value = 0
    }
  } catch (e) {
    sessionTableRows.value = []
    sessionTotal.value = 0
  } finally {
    sessionDrawerLoading.value = false
  }
}

const handleSessionRowClick = (row: ChargerConnectorSession) => {
  if (row?.sessionId) {
    selectSession(row.sessionId)
  }
}

const selectSession = async (sessionId: string) => {
  selectedSessionId.value = sessionId
  await reloadCurve()
}

const reloadCurve = async () => {
  const chargerId = Number(detail.value.id)
  const connectorNo = sessionDrawerConnectorNo.value
  const sessionId = selectedSessionId.value
  if (!chargerId || !connectorNo || !sessionId) return

  const from = curveTimeRange.value?.[0]
  const to = curveTimeRange.value?.[1]

  curveLoading.value = true
  try {
    const resp = await getChargerConnectorSessionCurve(chargerId, connectorNo, sessionId, {
      from,
      to,
      current: 1,
      size: 2000
    })
    if (resp.code === 200 && resp.data) {
      curvePoints.value = resp.data.records || []
    } else {
      curvePoints.value = []
    }
  } catch (e) {
    curvePoints.value = []
  } finally {
    curveLoading.value = false
  }

  await nextTick()
  renderCurveChart()
}

const renderCurveChart = () => {
  const el = curveChartRef.value
  if (!el) return
  if (!curveChart.value) {
    curveChart.value = echarts.init(el)
  }

  const x = curvePoints.value.map((p) => p.sampleTime)
  const voltage = curvePoints.value.map((p) => (p.voltage ?? null) as any)
  const current = curvePoints.value.map((p) => (p.current ?? null) as any)
  const power = curvePoints.value.map((p) => (p.power ?? null) as any)
  const soc = curvePoints.value.map((p) => (p.soc ?? null) as any)
  const energy = curvePoints.value.map((p) => (p.energy ?? null) as any)

  curveChart.value.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['电压(V)', '电流(A)', '功率(kW)', 'SOC(%)', '电量(kWh)'] },
    grid: { left: 60, right: 60, top: 40, bottom: 50 },
    xAxis: {
      type: 'category',
      data: x,
      axisLabel: { formatter: (v: string) => (v ? String(v).replace('T', ' ') : '') }
    },
    yAxis: [
      { type: 'value', name: 'V', position: 'left' },
      { type: 'value', name: 'A', position: 'right' },
      { type: 'value', name: 'kW', position: 'right', offset: 40 },
      { type: 'value', name: '%', position: 'left', offset: 40, min: 0, max: 100 },
      { type: 'value', name: 'kWh', position: 'left', offset: 80 }
    ],
    series: [
      { name: '电压(V)', type: 'line', yAxisIndex: 0, showSymbol: false, data: voltage },
      { name: '电流(A)', type: 'line', yAxisIndex: 1, showSymbol: false, data: current },
      { name: '功率(kW)', type: 'line', yAxisIndex: 2, showSymbol: false, data: power },
      { name: 'SOC(%)', type: 'line', yAxisIndex: 3, showSymbol: false, data: soc },
      { name: '电量(kWh)', type: 'line', yAxisIndex: 4, showSymbol: false, data: energy }
    ]
  })
  curveChart.value.resize()
}

watch(sessionDrawerVisible, async (visible) => {
  if (!visible) {
    curveChart.value?.dispose()
    curveChart.value = null
    return
  }
  await nextTick()
  renderCurveChart()
})

onBeforeUnmount(() => {
  curveChart.value?.dispose()
  curveChart.value = null
})

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
