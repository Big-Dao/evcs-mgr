<template>
  <div class="reconciliation-exception">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>对账异常处理</span>
          <el-button 
            type="primary" 
            :disabled="selectedIds.length === 0"
            @click="handleBatchResolve"
          >
            <el-icon><CircleCheck /></el-icon>
            批量处理
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="异常类型">
          <el-select v-model="searchForm.exceptionType" placeholder="请选择" clearable>
            <el-option label="交易缺失" value="MISSING_TRANSACTION" />
            <el-option label="金额不一致" value="AMOUNT_MISMATCH" />
            <el-option label="状态不一致" value="STATUS_MISMATCH" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.handleStatus" placeholder="请选择" clearable>
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table 
        :data="tableData" 
        v-loading="loading" 
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskNo" label="任务编号" width="150" />
        <el-table-column prop="exceptionType" label="异常类型" width="120">
          <template #default="{ row }">
            {{ getExceptionTypeText(row.exceptionType) }}
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="交易号" width="150" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="amount" label="系统金额" width="100">
          <template #default="{ row }">
            ¥{{ row.amount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="channelAmount" label="渠道金额" width="100">
          <template #default="{ row }">
            <span v-if="row.channelAmount">¥{{ row.channelAmount.toFixed(2) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="系统状态" width="100" />
        <el-table-column prop="channelStatus" label="渠道状态" width="100" />
        <el-table-column prop="handleStatus" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getHandleStatusType(row.handleStatus)" size="small">
              {{ getHandleStatusText(row.handleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.handleStatus === 'PENDING'"
              link 
              type="primary" 
              size="small" 
              @click="handleResolve(row)"
            >
              处理
            </el-button>
            <el-button link type="info" size="small" @click="handleViewDetail(row)">详情</el-button>
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

    <!-- 处理异常对话框 -->
    <el-dialog v-model="resolveDialogVisible" title="处理对账异常" width="600px">
      <el-form :model="resolveForm" label-width="100px">
        <el-form-item label="异常ID">
          <el-input v-model="currentException.id" disabled />
        </el-form-item>
        <el-form-item label="交易号">
          <el-input v-model="currentException.tradeNo" disabled />
        </el-form-item>
        <el-form-item label="异常类型">
          <el-input :value="getExceptionTypeText(currentException.exceptionType)" disabled />
        </el-form-item>
        <el-form-item label="异常详情">
          <el-input 
            v-model="currentException.exceptionDetail" 
            type="textarea"
            :rows="3"
            disabled 
          />
        </el-form-item>
        <el-form-item label="处理结果" required>
          <el-select v-model="resolveForm.handleResult" placeholder="请选择" style="width: 100%;">
            <el-option label="已修正" value="CORRECTED" />
            <el-option label="已补单" value="SUPPLEMENTED" />
            <el-option label="确认无误" value="CONFIRMED" />
            <el-option label="忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="resolveForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmResolve">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 批量处理对话框 -->
    <el-dialog v-model="batchResolveDialogVisible" title="批量处理异常" width="500px">
      <el-form :model="batchResolveForm" label-width="100px">
        <el-form-item label="选中数量">
          <el-input :value="selectedIds.length + ' 条'" disabled />
        </el-form-item>
        <el-form-item label="处理结果" required>
          <el-select v-model="batchResolveForm.handleResult" placeholder="请选择" style="width: 100%;">
            <el-option label="已修正" value="CORRECTED" />
            <el-option label="已补单" value="SUPPLEMENTED" />
            <el-option label="确认无误" value="CONFIRMED" />
            <el-option label="忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="batchResolveForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchResolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmBatchResolve">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 异常详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="异常详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="异常ID">{{ currentException.id }}</el-descriptions-item>
        <el-descriptions-item label="任务编号">{{ currentException.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="异常类型">
          {{ getExceptionTypeText(currentException.exceptionType) }}
        </el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="getHandleStatusType(currentException.handleStatus)">
            {{ getHandleStatusText(currentException.handleStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="交易号" :span="2">{{ currentException.tradeNo }}</el-descriptions-item>
        <el-descriptions-item label="渠道交易号" :span="2">{{ currentException.channelTradeNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单号" :span="2">{{ currentException.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="系统金额">¥{{ currentException.amount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="渠道金额">
          {{ currentException.channelAmount ? '¥' + currentException.channelAmount.toFixed(2) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="系统状态">{{ currentException.status }}</el-descriptions-item>
        <el-descriptions-item label="渠道状态">{{ currentException.channelStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="异常详情" :span="2">
          {{ currentException.exceptionDetail }}
        </el-descriptions-item>
        <el-descriptions-item label="处理结果" :span="2" v-if="currentException.handleResult">
          {{ currentException.handleResult }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间" :span="2" v-if="currentException.handleTime">
          {{ currentException.handleTime }}
        </el-descriptions-item>
        <el-descriptions-item label="处理人" :span="2" v-if="currentException.handledBy">
          {{ currentException.handledBy }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentException.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { 
  getReconciliationExceptionList,
  handleReconciliationException,
  batchHandleExceptions
} from '@/api/reconciliation'
import type { ReconciliationException, ExceptionQueryParams, HandleExceptionRequest } from '@/api/reconciliation'

const route = useRoute()
const loading = ref(false)
const resolveDialogVisible = ref(false)
const batchResolveDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const dateRange = ref<[string, string] | null>(null)
const selectedIds = ref<number[]>([])

const searchForm = reactive<ExceptionQueryParams>({
  taskId: undefined,
  exceptionType: '',
  handleStatus: '',
  startDate: '',
  endDate: '',
  current: 1,
  size: 10
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<ReconciliationException[]>([])
const currentException = ref<ReconciliationException>({} as ReconciliationException)

const resolveForm = reactive<HandleExceptionRequest>({
  exceptionId: 0,
  handleResult: '',
  remark: ''
})

const batchResolveForm = reactive({
  handleResult: '',
  remark: ''
})

// 从路由参数获取taskId
if (route.query.taskId) {
  searchForm.taskId = Number(route.query.taskId)
}

// 加载异常列表
const loadExceptionList = async () => {
  loading.value = true
  try {
    const params: ExceptionQueryParams = {
      ...searchForm,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getReconciliationExceptionList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载异常列表失败:', error)
    ElMessage.warning('加载异常列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        taskId: 1,
        taskNo: 'REC20241218001',
        exceptionType: 'AMOUNT_MISMATCH',
        tradeNo: 'TRD20241217123',
        channelTradeNo: '2024121722001234567890123',
        orderNo: 'ORD20241217123',
        amount: 68.50,
        channelAmount: 68.70,
        status: 'SUCCESS',
        channelStatus: 'SUCCESS',
        exceptionDetail: '金额不一致：系统金额68.50元，渠道金额68.70元，差额0.20元',
        handleStatus: 'PENDING',
        tenantId: 1,
        createTime: '2024-12-18 02:05:15'
      },
      {
        id: 2,
        taskId: 1,
        taskNo: 'REC20241218001',
        exceptionType: 'STATUS_MISMATCH',
        tradeNo: 'TRD20241217156',
        channelTradeNo: '2024121722001234567890456',
        orderNo: 'ORD20241217156',
        amount: 45.00,
        channelAmount: 45.00,
        status: 'PENDING',
        channelStatus: 'SUCCESS',
        exceptionDetail: '状态不一致：系统状态为PENDING，渠道状态为SUCCESS',
        handleStatus: 'PENDING',
        tenantId: 1,
        createTime: '2024-12-18 02:05:20'
      }
    ]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const getExceptionTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    MISSING_TRANSACTION: '交易缺失',
    AMOUNT_MISMATCH: '金额不一致',
    STATUS_MISMATCH: '状态不一致',
    OTHER: '其他'
  }
  return typeMap[type] || type
}

const getHandleStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    PENDING: 'warning',
    PROCESSING: 'primary',
    RESOLVED: 'success',
    IGNORED: 'info'
  }
  return typeMap[status] || 'info'
}

const getHandleStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    IGNORED: '已忽略'
  }
  return textMap[status] || status
}

const handleDateChange = (value: [string, string] | null) => {
  if (value) {
    searchForm.startDate = value[0]
    searchForm.endDate = value[1]
  } else {
    searchForm.startDate = ''
    searchForm.endDate = ''
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadExceptionList()
}

const handleReset = () => {
  searchForm.exceptionType = ''
  searchForm.handleStatus = ''
  searchForm.startDate = ''
  searchForm.endDate = ''
  dateRange.value = null
  loadExceptionList()
}

const handleSelectionChange = (selection: ReconciliationException[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleResolve = (row: ReconciliationException) => {
  currentException.value = row
  resolveForm.exceptionId = row.id
  resolveForm.handleResult = ''
  resolveForm.remark = ''
  resolveDialogVisible.value = true
}

const handleConfirmResolve = async () => {
  if (!resolveForm.handleResult) {
    ElMessage.warning('请选择处理结果')
    return
  }

  try {
    await handleReconciliationException(resolveForm)
    ElMessage.success('异常处理成功')
    resolveDialogVisible.value = false
    loadExceptionList()
  } catch (error) {
    console.error('处理异常失败:', error)
    ElMessage.error('处理异常失败')
  }
}

const handleBatchResolve = () => {
  batchResolveForm.handleResult = ''
  batchResolveForm.remark = ''
  batchResolveDialogVisible.value = true
}

const handleConfirmBatchResolve = async () => {
  if (!batchResolveForm.handleResult) {
    ElMessage.warning('请选择处理结果')
    return
  }

  try {
    await batchHandleExceptions(
      selectedIds.value,
      batchResolveForm.handleResult,
      batchResolveForm.remark
    )
    ElMessage.success(`成功处理 ${selectedIds.value.length} 条异常`)
    batchResolveDialogVisible.value = false
    selectedIds.value = []
    loadExceptionList()
  } catch (error) {
    console.error('批量处理异常失败:', error)
    ElMessage.error('批量处理异常失败')
  }
}

const handleViewDetail = (row: ReconciliationException) => {
  currentException.value = row
  detailDialogVisible.value = true
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadExceptionList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadExceptionList()
}

// 页面加载时获取数据
onMounted(() => {
  loadExceptionList()
})
</script>

<style scoped>
.reconciliation-exception {
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
