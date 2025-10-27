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
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="空闲" :value="1" />
            <el-option label="充电中" :value="2" />
            <el-option label="故障" :value="3" />
            <el-option label="离线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="桩ID" width="80" />
        <el-table-column prop="chargerCode" label="桩编号" width="120" />
        <el-table-column prop="stationId" label="充电站ID" width="100" />
        <el-table-column prop="chargerType" label="桩类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.chargerType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="power" label="功率(kW)" width="100" />
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
        <el-table-column prop="voltage" label="电压(V)" width="100" />
        <el-table-column prop="current" label="电流(A)" width="100" />
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
import { getChargerList, deleteCharger } from '@/api/charger'
import type { Charger, ChargerQueryParams } from '@/api/charger'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive<ChargerQueryParams>({
  chargerCode: '',
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

// 加载充电桩列表
const loadChargerList = async () => {
  loading.value = true
  try {
    const params: ChargerQueryParams = {
      chargerCode: searchForm.chargerCode,
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
        chargerCode: 'CH001',
        stationId: 1,
        chargerType: 'DC',
        power: 120,
        voltage: 380,
        current: 250,
        status: 1,
        connectorType: 'GB/T',
        tenantId: 1
      },
      {
        id: 2,
        chargerCode: 'CH002',
        stationId: 1,
        chargerType: 'AC',
        power: 7,
        voltage: 220,
        current: 32,
        status: 2,
        connectorType: 'GB/T',
        tenantId: 1
      }
    ] as Charger[]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

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

const handleSearch = () => {
  pagination.currentPage = 1
  loadChargerList()
}

const handleReset = () => {
  searchForm.chargerCode = ''
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
