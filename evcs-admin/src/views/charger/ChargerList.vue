<template>
  <div class="charger-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>充电桩列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增充电桩
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="充电桩编号">
          <el-input v-model="searchForm.chargerCode" placeholder="请输入充电桩编号" clearable />
        </el-form-item>
        <el-form-item label="桩类型">
          <el-select v-model="searchForm.chargerType" placeholder="请选择" clearable style="width: 120px;">
            <el-option label="直流快充" :value="1" />
            <el-option label="交流慢充" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="空闲" :value="1" />
            <el-option label="充电中" :value="2" />
            <el-option label="故障" :value="3" />
            <el-option label="离线" :value="0" />
            <el-option label="维护" :value="4" />
            <el-option label="预约中" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" @expand-change="handleExpandChange">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="margin: 10px 0;">
              <el-table
                v-if="getRowConnectors(row).length > 0"
                :data="getRowConnectors(row)"
                size="small"
                border
                style="width: 100%"
                v-loading="connectorLoadingMap[row.id]"
              >
                <el-table-column prop="connectorNo" label="枪口号" width="90" />
                <el-table-column prop="connectorType" label="枪口类型" min-width="160">
                  <template #default="{ row: c }">
                    <span>{{ c.connectorType || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="110">
                  <template #default="{ row: c }">
                    <el-tag :type="getStatusType(c.status)" size="small">{{ getStatusText(c.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="faultCode" label="故障码" width="110">
                  <template #default="{ row: c }">
                    <span>{{ c.faultCode || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="faultDescription" label="故障描述" min-width="220" show-overflow-tooltip>
                  <template #default="{ row: c }">
                    <span>{{ c.faultDescription || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="currentSessionId" label="会话ID" min-width="160" show-overflow-tooltip>
                  <template #default="{ row: c }">
                    <span>{{ c.currentSessionId || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="chargingStartTime" label="开始充电" min-width="180">
                  <template #default="{ row: c }">
                    <span>{{ c.chargingStartTime || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="chargedEnergy" label="已充电量" width="110">
                  <template #default="{ row: c }">
                    <span>{{ c.chargedEnergy ?? '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="chargedDuration" label="已充时长" width="110">
                  <template #default="{ row: c }">
                    <span>{{ c.chargedDuration ?? '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="180">
                  <template #default="{ row: c }">
                    <span>{{ c.lastHeartbeat || '-' }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <span v-else style="color:#909399;">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="桩ID" width="90" />
        <el-table-column prop="chargerCode" label="桩编号" width="160" />
        <el-table-column prop="chargerName" label="桩名称" min-width="160" />
        <el-table-column prop="stationId" label="充电站ID" width="110" />
        <el-table-column prop="chargerType" label="桩类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ getChargerTypeText(row.chargerType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ratedPower" label="额定功率(kW)" width="130" />
        <el-table-column prop="inputVoltage" label="输入电压(V)" width="130">
          <template #default="{ row }">
            <span>{{ row.inputVoltage ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="gunCount" label="枪头数" width="90" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              <el-icon>
                <component :is="getStatusIcon(row.status)" />
              </el-icon>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="180" />
        <el-table-column prop="enabled" label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChargerList, deleteCharger, getChargerConnectors } from '@/api/charger'
import type { Charger, ChargerQueryParams, ChargerConnector } from '@/api/charger'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive<ChargerQueryParams>({
  chargerCode: '',
  chargerType: undefined,
  status: undefined,
  current: 1,
  size: 10
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<Charger[]>([])

const connectorCache = reactive<Record<number, ChargerConnector[]>>({})
const connectorLoadingMap = reactive<Record<number, boolean>>({})

const ensureConnectorsLoaded = async (chargerId: number) => {
  if (!chargerId) return
  if (connectorCache[chargerId]) return
  connectorLoadingMap[chargerId] = true
  try {
    const resp = await getChargerConnectors(chargerId)
    if (resp.code === 200 && Array.isArray(resp.data)) {
      connectorCache[chargerId] = resp.data
    } else {
      connectorCache[chargerId] = []
    }
  } catch (e) {
    connectorCache[chargerId] = []
  } finally {
    connectorLoadingMap[chargerId] = false
  }
}

const handleExpandChange = async (row: Charger, expandedRows: Charger[]) => {
  const expanded = Array.isArray(expandedRows) && expandedRows.some((r) => r.id === row.id)
  if (expanded) {
    await ensureConnectorsLoaded(row.id)
  }
}

// 加载充电桩列表
const loadChargerList = async () => {
  loading.value = true
  try {
    const params: ChargerQueryParams = {
      chargerCode: searchForm.chargerCode,
      chargerType: searchForm.chargerType,
      status: searchForm.status,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getChargerList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载充电桩列表失败:', error)
    ElMessage.warning('加载充电桩列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        chargerCode: 'ST001-DC-001',
        chargerName: '1号快充桩',
        stationId: 1,
        stationCode: 'ST001',
        chargerType: 1,
        ratedPower: 120,
        inputVoltage: 380,
        gunCount: 2,
        gunTypes: 'CCS,CHAdeMO',
        status: 1,
        enabled: 1,
        tenantId: 1,
        lastHeartbeat: '-'
      },
      {
        id: 2,
        chargerCode: 'ST001-AC-001',
        chargerName: '4号慢充桩',
        stationId: 1,
        stationCode: 'ST001',
        chargerType: 2,
        ratedPower: 7,
        inputVoltage: 220,
        gunCount: 1,
        gunTypes: 'Type2',
        status: 2,
        enabled: 1,
        tenantId: 1,
        lastHeartbeat: '-'
      }
    ] as Charger[]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const getChargerTypeText = (chargerType?: number) => {
  const textMap: Record<number, string> = {
    1: '直流快充',
    2: '交流慢充'
  }
  if (typeof chargerType !== 'number') return '未知'
  return textMap[chargerType] || '未知'
}

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
    const connectorType = types.length === count ? types[idx] : (types.length === 1 ? types[0] : undefined)
    return { connectorNo, connectorType }
  })
}

const getRowConnectors = (row: Charger): ChargerConnector[] => {
  const cached = connectorCache[row.id]
  if (Array.isArray(cached)) {
    return cached
  }
  // fallback：仅用于接口不可用时的展示，不再把 charger.status 当作枪口状态
  return buildGunList(row.gunCount, row.gunTypes).map((x) =>
    ({
      id: 0,
      chargerId: row.id,
      connectorNo: x.connectorNo,
      connectorType: x.connectorType,
      status: -1,
      lastHeartbeat: row.lastHeartbeat
    }) as ChargerConnector
  )
}

const getStatusType = (status?: number) => {
  if (typeof status !== 'number' || status < 0) return 'info'
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
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
    4: 'Tools',
    5: 'Bell',
    0: 'WarningFilled'
  }
  return iconMap[status] || 'QuestionFilled'
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadChargerList()
}

const handleReset = () => {
  searchForm.chargerCode = ''
  searchForm.chargerType = undefined
  searchForm.status = undefined
  loadChargerList()
}

const handleAdd = () => {
  ElMessage.info('新增充电桩功能')
}

const handleView = (row: Charger) => {
  router.push(`/chargers/${row.id}`)
}

const handleEdit = (row: Charger) => {
  ElMessage.info('编辑充电桩: ' + row.chargerCode)
}

const handleDelete = async (row: Charger) => {
  try {
    await ElMessageBox.confirm('确定要删除该充电桩吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteCharger(row.id)
    ElMessage.success('删除成功')
    loadChargerList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadChargerList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadChargerList()
}

// 页面加载时获取数据
onMounted(() => {
  loadChargerList()
})
</script>

<style scoped>
.charger-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
