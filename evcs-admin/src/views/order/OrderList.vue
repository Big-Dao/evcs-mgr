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
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="充电中" value="CHARGING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="订单ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" width="150" />
        <el-table-column prop="stationName" label="充电站" />
        <el-table-column prop="chargerCode" label="充电桩" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="chargingAmount" label="电量(kWh)" width="100" />
        <el-table-column prop="totalAmount" label="金额(元)" width="100">
          <template #default="{ row }">
            ¥{{ row.totalAmount.toFixed(2) }}
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
            <el-button v-if="row.status === 'COMPLETED'" link type="success" size="small" @click="handleReconcile(row)">对账</el-button>
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
import { ElMessage } from 'element-plus'
import { getOrderList, exportOrders } from '@/api/order'
import type { Order, OrderQueryParams } from '@/api/order'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive<OrderQueryParams>({
  orderNo: '',
  stationId: undefined,
  status: undefined,
  startTime: '',
  endTime: '',
  current: 1,
  size: 10
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<Order[]>([])

// 加载订单列表
const loadOrderList = async () => {
  loading.value = true
  try {
    const params: OrderQueryParams = {
      orderNo: searchForm.orderNo,
      stationId: searchForm.stationId,
      status: searchForm.status,
      startTime: searchForm.startTime,
      endTime: searchForm.endTime,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getOrderList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载订单列表失败:', error)
    ElMessage.warning('加载订单列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        orderNo: 'ORD20241011001',
        userId: 1,
        userName: '张三',
        stationId: 1,
        stationName: '市中心充电站',
        chargerId: 1,
        chargerCode: 'CH001',
        startTime: '2024-10-11 10:30:00',
        endTime: '2024-10-11 12:00:00',
        chargingDuration: 90,
        chargingAmount: 45.8,
        totalAmount: 68.70,
        status: 'COMPLETED',
        tenantId: 1,
        createTime: '2024-10-11 10:30:00'
      },
      {
        id: 2,
        orderNo: 'ORD20241011002',
        userId: 1,
        userName: '张三',
        stationId: 2,
        stationName: '高新区充电站',
        chargerId: 2,
        chargerCode: 'CH002',
        startTime: '2024-10-11 11:00:00',
        chargingDuration: 30,
        chargingAmount: 12.5,
        totalAmount: 18.75,
        status: 'CHARGING',
        tenantId: 1,
        createTime: '2024-10-11 11:00:00'
      }
    ]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    CHARGING: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    CHARGING: '充电中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return textMap[status] || '未知'
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadOrderList()
}

const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.stationId = undefined
  searchForm.status = undefined
  searchForm.startTime = ''
  searchForm.endTime = ''
  loadOrderList()
}

const handleView = (row: Order) => {
  router.push(`/orders/${row.id}`)
}

const handleReconcile = (row: Order) => {
  ElMessage.success('订单对账: ' + row.orderNo)
}

const handleExport = async () => {
  try {
    await exportOrders(searchForm)
    ElMessage.success('导出订单数据成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadOrderList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadOrderList()
}

// 页面加载时获取数据
onMounted(() => {
  loadOrderList()
})
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
