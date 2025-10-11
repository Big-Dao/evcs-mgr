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
          <el-input v-model="searchForm.code" placeholder="请输入充电桩编号" clearable />
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

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="chargerId" label="桩ID" width="80" />
        <el-table-column prop="chargerCode" label="桩编号" width="120" />
        <el-table-column prop="stationName" label="所属充电站" />
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
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const searchForm = reactive({
  code: '',
  status: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 256
})

const tableData = ref([
  {
    chargerId: 1,
    chargerCode: 'CH001',
    stationName: '市中心充电站',
    chargerType: 'DC快充',
    power: 120,
    voltage: 380,
    current: 250,
    status: 1
  },
  {
    chargerId: 2,
    chargerCode: 'CH002',
    stationName: '市中心充电站',
    chargerType: 'AC慢充',
    power: 7,
    voltage: 220,
    current: 32,
    status: 2
  },
  {
    chargerId: 3,
    chargerCode: 'CH003',
    stationName: '高新区充电站',
    chargerType: 'DC快充',
    power: 60,
    voltage: 380,
    current: 125,
    status: 3
  }
])

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
  ElMessage.success('查询成功')
}

const handleReset = () => {
  searchForm.code = ''
  searchForm.status = ''
}

const handleAdd = () => {
  ElMessage.info('新增充电桩功能')
}

const handleView = (row: any) => {
  ElMessage.info('查看充电桩详情: ' + row.chargerCode)
}

const handleEdit = (row: any) => {
  ElMessage.info('编辑充电桩: ' + row.chargerCode)
}

const handleDelete = (_row: any) => {
  ElMessageBox.confirm('确定要删除该充电桩吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  })
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
}
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
