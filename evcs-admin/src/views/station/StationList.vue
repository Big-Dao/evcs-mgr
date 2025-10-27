<template>
  <div class="station-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>充电站列表</span>
          <el-button-group>
            <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">
              <el-icon><List /></el-icon>
              列表
            </el-button>
            <el-button :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">
              <el-icon><Grid /></el-icon>
              卡片
            </el-button>
          </el-button-group>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增充电站
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="充电站名称">
          <el-input v-model="searchForm.stationName" placeholder="请输入充电站名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="运营中" :value="1" />
            <el-option label="维护中" :value="2" />
            <el-option label="已停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- List View -->
      <el-table v-if="viewMode === 'list'" :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="站点ID" width="80" />
        <el-table-column prop="stationCode" label="站点编码" width="120" />
        <el-table-column prop="stationName" label="站点名称" />
        <el-table-column prop="address" label="地址" />
        <el-table-column prop="totalChargers" label="充电桩数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
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

      <!-- Card View -->
      <el-row v-else :gutter="20" v-loading="loading">
        <el-col v-for="station in tableData" :key="station.id" :span="8" style="margin-bottom: 20px;">
          <el-card class="station-card" shadow="hover">
            <template #header>
              <div class="station-card-header">
                <span>{{ station.stationName }}</span>
                <el-tag :type="getStatusType(station.status)" size="small">
                  {{ getStatusText(station.status) }}
                </el-tag>
              </div>
            </template>
            <div class="station-info">
              <p><el-icon><LocationInformation /></el-icon> {{ station.address }}</p>
              <p><el-icon><Monitor /></el-icon> 充电桩: {{ station.totalChargers || 0 }} 个</p>
              <p><el-icon><Odometer /></el-icon> 编码: {{ station.stationCode }}</p>
            </div>
            <div class="station-actions">
              <el-button size="small" @click="handleView(station)">详情</el-button>
              <el-button size="small" type="primary" @click="handleEdit(station)">编辑</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

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
import { getStationList, deleteStation } from '@/api/station'
import type { Station, StationQueryParams } from '@/api/station'

const router = useRouter()
const loading = ref(false)

const viewMode = ref('list')

const searchForm = reactive<StationQueryParams>({
  stationName: '',
  status: undefined,
  current: 1,
  size: 10
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<Station[]>([])

// 加载充电站列表
const loadStationList = async () => {
  loading.value = true
  try {
    const params: StationQueryParams = {
      stationName: searchForm.stationName,
      status: searchForm.status,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getStationList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载充电站列表失败:', error)
    ElMessage.warning('加载充电站列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        stationCode: 'ST001',
        stationName: '市中心充电站',
        province: '浙江省',
        city: '杭州市',
        district: '西湖区',
        address: '市中心区人民路123号',
        status: 1,
        tenantId: 1
      },
      {
        id: 2,
        stationCode: 'ST002',
        stationName: '高新区充电站',
        province: '浙江省',
        city: '杭州市',
        district: '滨江区',
        address: '高新区科技大道456号',
        status: 1,
        tenantId: 1
      }
    ] as Station[]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

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

const handleSearch = () => {
  pagination.currentPage = 1
  loadStationList()
}

const handleReset = () => {
  searchForm.stationName = ''
  searchForm.status = undefined
  loadStationList()
}

const handleAdd = () => {
  ElMessage.info('新增充电站功能')
}

const handleView = (row: Station) => {
  router.push(`/stations/${row.id}`)
}

const handleEdit = (row: Station) => {
  ElMessage.info('编辑充电站: ' + row.stationName)
}

const handleDelete = async (row: Station) => {
  try {
    await ElMessageBox.confirm('确定要删除该充电站吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteStation(row.id)
    ElMessage.success('删除成功')
    loadStationList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadStationList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadStationList()
}

// 页面加载时获取数据
onMounted(() => {
  loadStationList()
})
</script>

<style scoped>
.station-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
}

.station-card {
  height: 100%;
}

.station-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.station-info {
  margin-bottom: 15px;
}

.station-info p {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #606266;
}

.station-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>
