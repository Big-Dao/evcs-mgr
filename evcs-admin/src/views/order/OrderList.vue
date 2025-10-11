<template>
  <div class="order-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单列表</span>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出Excel
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="充电站">
          <el-input v-model="searchForm.station" placeholder="请输入充电站名称" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="充电中" value="charging" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="orderId" label="订单ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="stationName" label="充电站" />
        <el-table-column prop="chargerCode" label="充电桩" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="energy" label="电量(kWh)" width="100" />
        <el-table-column prop="amount" label="金额(元)" width="100">
          <template #default="{ row }">
            ¥{{ row.amount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 'completed'" link type="success" size="small" @click="handleReconcile(row)">对账</el-button>
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
import { ElMessage } from 'element-plus'

const searchForm = reactive({
  orderNo: '',
  station: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 500
})

const tableData = ref([
  {
    orderId: 1,
    orderNo: 'ORD20241011001',
    stationName: '市中心充电站',
    chargerCode: 'CH001',
    startTime: '2024-10-11 10:30:00',
    endTime: '2024-10-11 12:00:00',
    energy: 45.8,
    amount: 68.70,
    status: 'completed'
  },
  {
    orderId: 2,
    orderNo: 'ORD20241011002',
    stationName: '高新区充电站',
    chargerCode: 'CH002',
    startTime: '2024-10-11 11:00:00',
    endTime: '',
    energy: 12.5,
    amount: 18.75,
    status: 'charging'
  }
])

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    charging: 'warning',
    completed: 'success',
    cancelled: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    charging: '充电中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return textMap[status] || '未知'
}

const handleSearch = () => {
  pagination.currentPage = 1
  ElMessage.success('查询成功')
}

const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.station = ''
  searchForm.status = ''
  searchForm.dateRange = []
}

const handleView = (row: any) => {
  ElMessage.info('查看订单详情: ' + row.orderNo)
}

const handleReconcile = (row: any) => {
  ElMessage.success('订单对账: ' + row.orderNo)
}

const handleExport = () => {
  ElMessage.success('导出订单数据')
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
}
</script>

<style scoped>
.order-list {
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
