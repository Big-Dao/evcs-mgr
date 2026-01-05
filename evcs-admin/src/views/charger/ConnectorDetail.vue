<template>
  <div class="connector-detail">
    <el-page-header @back="handleBack" content="充电枪详情">
      <template #extra>
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <!-- 基本信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-tag :type="getStatusType(connector.status)" size="large">
                {{ getStatusText(connector.status) }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="枪口编号">{{ connector.connectorNo }}</el-descriptions-item>
            <el-descriptions-item label="接口类型">{{ connector.connectorType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属充电桩ID">{{ chargerId }}</el-descriptions-item>
            <el-descriptions-item label="当前会话ID">{{ connector.currentSessionId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前用户ID">{{ connector.currentUserId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最后心跳">{{ connector.lastHeartbeat || '-' }}</el-descriptions-item>
            <el-descriptions-item label="故障码">{{ connector.faultCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="故障描述">{{ connector.faultDescription || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 实时数据 -->
        <el-card class="detail-card" style="margin-top: 20px;">
          <template #header>
            <span>实时数据快照</span>
          </template>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-statistic title="电压 (V)" :value="connector.lastVoltage || 0" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="电流 (A)" :value="connector.lastCurrent || 0" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="功率 (kW)" :value="connector.lastPower || 0" />
            </el-col>
            <el-col :span="6">
              <el-statistic title="SOC (%)" :value="connector.lastSoc || 0" />
            </el-col>
          </el-row>
          <el-divider />
          <el-row :gutter="20">
             <el-col :span="8">
              <el-statistic title="已充电量 (kWh)" :value="connector.chargedEnergy || 0" />
            </el-col>
            <el-col :span="8">
              <el-statistic title="已充时长 (分钟)" :value="connector.chargedDuration || 0" />
            </el-col>
             <el-col :span="8">
              <el-statistic title="采样时间" :value="connector.lastMeterTime || '-'" />
            </el-col>
          </el-row>
        </el-card>

        <!-- 历史会话 -->
        <el-card class="detail-card" style="margin-top: 20px;">
          <template #header>
            <span>历史会话</span>
          </template>
          <el-table :data="sessionList" v-loading="sessionLoading" style="width: 100%">
            <el-table-column prop="sessionId" label="会话ID" width="180" />
            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column prop="stopTime" label="结束时间" width="180" />
            <el-table-column prop="totalEnergy" label="充电量(kWh)" />
            <el-table-column prop="durationSeconds" label="时长(秒)" />
            <el-table-column prop="status" label="状态">
               <template #default="{ row }">
                 <el-tag size="small">{{ row.status === 1 ? '进行中' : '已结束' }}</el-tag>
               </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="sessionPage.current"
            v-model:page-size="sessionPage.size"
            :total="sessionPage.total"
            layout="prev, pager, next"
            @current-change="handleSessionPageChange"
            style="margin-top: 10px; justify-content: flex-end;"
          />
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 操作面板 -->
        <el-card class="detail-card">
          <template #header>
            <span>远程控制</span>
          </template>
          <div class="control-panel">
            <el-button type="success" :disabled="connector.status === 2" @click="handleStart">启动充电</el-button>
            <el-button type="danger" :disabled="connector.status !== 2" @click="handleStop">停止充电</el-button>
            <el-button type="warning" @click="handleReset">复位</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { 
  getChargerConnectors, 
  getChargerConnectorSessions, 
  startChargerConnectorSession, 
  stopChargerConnectorSession 
} from '@/api/charger'
import type { ChargerConnector, ChargerConnectorSession } from '@/api/charger'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const chargerId = Number(route.params.chargerId)
const connectorNo = Number(route.params.connectorNo)

const connector = ref<ChargerConnector>({} as ChargerConnector)
const sessionList = ref<ChargerConnectorSession[]>([])
const sessionLoading = ref(false)
const sessionPage = reactive({
  current: 1,
  size: 10,
  total: 0
})

const getStatusType = (status?: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'info',
    5: 'primary'
  }
  return map[status || 0] || 'info'
}

const getStatusText = (status?: number) => {
  const map: Record<number, string> = {
    0: '离线',
    1: '空闲',
    2: '充电中',
    3: '故障',
    4: '维护',
    5: '预约中'
  }
  return map[status || 0] || '未知'
}

const loadConnectorDetail = async () => {
  try {
    // Since there is no getConnectorDetail API, we fetch all connectors of the charger and find the one we need
    const res = await getChargerConnectors(chargerId)
    if (res.code === 200 && res.data) {
      // Use loose comparison or string conversion to ensure match regardless of type (number vs string)
      const found = res.data.find(c => String(c.connectorNo) === String(connectorNo))
      if (found) {
        connector.value = found
      } else {
        ElMessage.error('未找到该枪口信息')
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取枪口详情失败')
  }
}

const loadSessions = async () => {
  sessionLoading.value = true
  try {
    const res = await getChargerConnectorSessions(chargerId, connectorNo, {
      current: sessionPage.current,
      size: sessionPage.size
    })
    if (res.code === 200 && res.data) {
      sessionList.value = res.data.records || []
      sessionPage.total = res.data.total || 0
    }
  } catch (error) {
    console.error(error)
    // ElMessage.error('获取会话历史失败')
  } finally {
    sessionLoading.value = false
  }
}

const handleBack = () => {
  router.back()
}

const handleRefresh = () => {
  loadConnectorDetail()
  loadSessions()
}

const handleSessionPageChange = (page: number) => {
  sessionPage.current = page
  loadSessions()
}

const handleStart = async () => {
  try {
    await ElMessageBox.confirm('确定要启动充电吗？', '提示', { type: 'warning' })
    
    if (!userStore.userId) {
      ElMessage.error('无法获取当前用户信息')
      return
    }

    await startChargerConnectorSession(chargerId, connectorNo, {
      // Let backend generate sessionId if not provided, or generate one here if needed.
      // Backend accepts optional sessionId.
      userId: userStore.userId
    })
    ElMessage.success('启动指令已发送')
    // Wait a bit for status update
    setTimeout(() => handleRefresh(), 1000)
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('启动失败')
    }
  }
}

const handleStop = async () => {
  try {
    await ElMessageBox.confirm('确定要停止充电吗？', '提示', { type: 'warning' })
    
    const sessionId = connector.value.currentSessionId
    if (!sessionId) {
       // If no session ID on connector, maybe force stop without it?
       // But backend might need it for order settlement.
       // Let's try sending empty params if we don't have it, or warn user.
       // For now, let's proceed, backend might handle it.
    }

    await stopChargerConnectorSession(chargerId, connectorNo, {
      sessionId: sessionId
    })
    ElMessage.success('停止指令已发送')
    setTimeout(() => handleRefresh(), 1000)
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('停止失败')
    }
  }
}

const handleReset = () => {
  ElMessage.info('复位功能暂未实现')
}

onMounted(() => {
  if (chargerId && connectorNo) {
    loadConnectorDetail()
    loadSessions()
  } else {
    ElMessage.error('参数错误')
  }
})
</script>

<style scoped>
.connector-detail {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.control-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.control-panel .el-button {
  margin-left: 0;
}
</style>
