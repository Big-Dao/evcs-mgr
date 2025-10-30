<template>
  <div class="station-detail">
    <el-page-header @back="handleBack" content="充电站详情">
      <template #extra>
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
      </template>
    </el-page-header>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-tag :type="getStatusType(detail.status)">
            {{ getStatusText(detail.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="站点ID">{{ detail.stationId }}</el-descriptions-item>
        <el-descriptions-item label="站点编码">{{ detail.stationCode }}</el-descriptions-item>
        <el-descriptions-item label="站点名称">{{ detail.stationName }}</el-descriptions-item>
        <el-descriptions-item label="所属租户">{{ detail.tenantName }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ detail.longitude }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ detail.latitude }}</el-descriptions-item>
        <el-descriptions-item label="服务时间">{{ detail.serviceTime }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">
          <el-tag v-for="method in detail.paymentMethods" :key="method" size="small" style="margin-right: 5px;">
            {{ method }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="设施" :span="2">
          <el-tag v-for="facility in detail.facilities" :key="facility" size="small" style="margin-right: 5px;">
            {{ facility }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card>
          <el-statistic title="充电桩总数" :value="detail.chargerCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="在线充电桩" :value="detail.onlineCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="今日订单数" :value="detail.todayOrders">
            <template #suffix>笔</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic title="今日营收" :value="detail.todayRevenue">
            <template #prefix>¥</template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>充电桩列表</span>
      </template>

      <el-table :data="chargers" style="width: 100%">
        <el-table-column prop="chargerCode" label="桩编号" width="120" />
        <el-table-column prop="chargerType" label="桩类型" width="100" />
        <el-table-column prop="power" label="功率(kW)" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getChargerStatusType(row.status)" size="small">
              {{ getChargerStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="voltage" label="电压(V)" width="100" />
        <el-table-column prop="current" label="电流(A)" width="100" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewCharger(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleEditCharger(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>位置地图</span>
      </template>

      <div id="map-container" style="height: 400px; background: #f5f5f5; display: flex; align-items: center; justify-content: center;">
        <div style="text-align: center; color: #999;">
          <el-icon :size="60"><Location /></el-icon>
          <p style="margin-top: 10px;">地图组件占位（需集成高德/百度地图）</p>
          <p style="font-size: 14px;">位置: {{ detail.latitude }}, {{ detail.longitude }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStationDetail } from '@/api/station'
import { getChargerList } from '@/api/charger'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const detail = ref({
  stationId: 0,
  stationCode: '',
  stationName: '',
  tenantName: '',
  address: '',
  contactName: '',
  contactPhone: '',
  longitude: '',
  latitude: '',
  serviceTime: '00:00-24:00',
  paymentMethods: [] as string[],
  facilities: [] as string[],
  status: 1,
  chargerCount: 0,
  onlineCount: 0,
  todayOrders: 0,
  todayRevenue: 0
})

const chargers = ref<any[]>([])

// 加载充电站详情
const loadStationDetail = async () => {
  try {
    loading.value = true
    const stationId = Number(route.params.id || route.query.id)
    if (!stationId) {
      ElMessage.error('充电站ID不能为空')
      return
    }

    const response = await getStationDetail(stationId)
    if (response.code === 200 && response.data) {
      const data: any = response.data
      detail.value = {
        stationId: data.id || stationId,
        stationCode: data.stationCode || '',
        stationName: data.stationName || '',
        tenantName: data.tenantName || '',
        address: `${data.province || ''}${data.city || ''}${data.district || ''}${data.address || ''}`,
        contactName: data.contactName || '',
        contactPhone: data.contactPhone || '',
        longitude: data.longitude?.toString() || '',
        latitude: data.latitude?.toString() || '',
        serviceTime: '00:00-24:00',
        paymentMethods: ['微信支付', '支付宝', '银联卡'],
        facilities: ['WiFi', '休息区'],
        status: data.status || 1,
        chargerCount: data.totalChargers || 0,
        onlineCount: data.availableChargers || 0,
        todayOrders: 0,
        todayRevenue: 0
      }

      // 加载充电桩列表
      await loadChargers(stationId)
    } else {
      ElMessage.error(response.message || '加载充电站详情失败')
    }
  } catch (error) {
    console.error('加载充电站详情失败:', error)
    ElMessage.error('加载充电站详情失败')
  } finally {
    loading.value = false
  }
}

// 加载充电桩列表
const loadChargers = async (stationId: number) => {
  try {
    const response = await getChargerList({ stationId, current: 1, size: 100 })
    if (response.code === 200 && response.data) {
      const records = (response.data as any).records || response.data
      chargers.value = Array.isArray(records) ? records : []
    }
  } catch (error) {
    console.error('加载充电桩列表失败:', error)
  }
}

onMounted(() => {
  loadStationDetail()
})

const getStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    0: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '运营中',
    2: '维护中',
    0: '已停用'
  }
  return textMap[status] || '未知'
}

const getChargerStatusType = (status: number) => {
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger',
    0: 'info'
  }
  return typeMap[status] || 'info'
}

const getChargerStatusText = (status: number) => {
  const textMap: Record<number, string> = {
    1: '空闲',
    2: '充电中',
    3: '故障',
    0: '离线'
  }
  return textMap[status] || '未知'
}

const handleBack = () => {
  router.back()
}

const handleEdit = () => {
  ElMessage.info('编辑充电站功能')
}

const handleViewCharger = (row: any) => {
  ElMessage.info('查看充电桩: ' + row.chargerCode)
}

const handleEditCharger = (row: any) => {
  ElMessage.info('编辑充电桩: ' + row.chargerCode)
}
</script>

<style scoped>
.station-detail {
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
</style>
