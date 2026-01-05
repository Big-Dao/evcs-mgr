<template>
  <div class="connector-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>充电枪列表</span>
          <!-- 暂时不提供新增充电枪功能，通常随充电桩一起添加 -->
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="枪口编号">
          <el-input v-model="searchForm.connectorNo" placeholder="请输入枪口编号" clearable />
        </el-form-item>
        <el-form-item label="所属桩编号">
          <el-input v-model="searchForm.chargerCode" placeholder="请输入充电桩编号" clearable />
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

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="connectorNo" label="枪口编号" width="120" />
        <el-table-column prop="chargerCode" label="所属充电桩" width="150" />
        <el-table-column prop="connectorType" label="接口类型" width="120">
           <template #default="{ row }">
             <el-tag>{{ row.connectorType }}</el-tag>
           </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="power" label="当前功率(kW)" width="120" />
        <el-table-column prop="voltage" label="电压(V)" width="100" />
        <el-table-column prop="current" label="电流(A)" width="100" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleRemoteStart(row)" :disabled="row.status !== 1">启动</el-button>
            <el-button link type="danger" size="small" @click="handleRemoteStop(row)" :disabled="row.status !== 2">停止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConnectorList, type ChargerConnector } from '@/api/charger'

const router = useRouter()

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  connectorNo: '',
  chargerCode: '',
  status: ''
})

const tableData = ref<ChargerConnector[]>([])

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'info',
    5: 'primary'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '离线',
    1: '空闲',
    2: '充电中',
    3: '故障',
    4: '维护',
    5: '预约中'
  }
  return map[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getConnectorList({
      current: currentPage.value,
      size: pageSize.value,
      connectorNo: searchForm.connectorNo || undefined,
      chargerCode: searchForm.chargerCode || undefined,
      status: searchForm.status ? Number(searchForm.status) : undefined
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handleReset = () => {
  searchForm.connectorNo = ''
  searchForm.chargerCode = ''
  searchForm.status = ''
  handleSearch()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  fetchData()
}

const handleDetail = (row: any) => {
  if (row.chargerId && row.connectorNo) {
    router.push(`/chargers/${row.chargerId}/connectors/${row.connectorNo}`)
  } else {
    ElMessage.warning('缺少必要参数，无法查看详情')
  }
}

const handleRemoteStart = (row: any) => {
  ElMessage.success(`发送启动指令: ${row.chargerCode}-${row.connectorNo}`)
}

const handleRemoteStop = (row: any) => {
  ElMessage.warning(`发送停止指令: ${row.chargerCode}-${row.connectorNo}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
