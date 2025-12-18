<template>
  <div class="reconciliation-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>对账任务管理</span>
          <div>
            <el-button type="primary" @click="handleExecute">
              <el-icon><CircleCheck /></el-icon>
              执行对账
            </el-button>
            <el-button type="success" @click="handleDailyExecute">
              <el-icon><Calendar /></el-icon>
              每日对账
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="支付渠道">
          <el-select v-model="searchForm.channel" placeholder="请选择" clearable>
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="待执行" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="对账日期">
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

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column prop="channel" label="支付渠道" width="100">
          <template #default="{ row }">
            {{ getChannelText(row.channel) }}
          </template>
        </el-table-column>
        <el-table-column prop="reconciliationDate" label="对账日期" width="120" />
        <el-table-column prop="totalCount" label="总笔数" width="100" />
        <el-table-column prop="matchedCount" label="匹配笔数" width="100">
          <template #default="{ row }">
            <span style="color: #67c23a;">{{ row.matchedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unmatchedCount" label="未匹配" width="100">
          <template #default="{ row }">
            <span v-if="row.unmatchedCount > 0" style="color: #e6a23c;">{{ row.unmatchedCount }}</span>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="exceptionCount" label="异常笔数" width="100">
          <template #default="{ row }">
            <span v-if="row.exceptionCount > 0" style="color: #f56c6c;">{{ row.exceptionCount }}</span>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="对账金额(元)" width="120">
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
        <el-table-column prop="duration" label="耗时(秒)" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewReport(row)">查看报告</el-button>
            <el-button 
              v-if="row.exceptionCount > 0"
              link 
              type="warning" 
              size="small" 
              @click="handleViewExceptions(row)"
            >
              处理异常
            </el-button>
            <el-button 
              v-if="row.status === 'FAILED'"
              link 
              type="success" 
              size="small" 
              @click="handleRetry(row)"
            >
              重试
            </el-button>
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

    <!-- 执行对账对话框 -->
    <el-dialog v-model="executeDialogVisible" title="执行对账" width="500px">
      <el-form :model="executeForm" label-width="100px">
        <el-form-item label="支付渠道" required>
          <el-select v-model="executeForm.channel" placeholder="请选择" style="width: 100%;">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="对账日期" required>
          <el-date-picker
            v-model="executeForm.reconciliationDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="对账单文件">
          <el-input
            v-model="executeForm.statementFileUrl"
            placeholder="对账单文件URL（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmExecute">确认执行</el-button>
      </template>
    </el-dialog>

    <!-- 每日对账对话框 -->
    <el-dialog v-model="dailyExecuteDialogVisible" title="每日对账" width="500px">
      <el-form :model="dailyExecuteForm" label-width="100px">
        <el-form-item label="支付渠道" required>
          <el-select v-model="dailyExecuteForm.channel" placeholder="请选择" style="width: 100%;">
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="对账日期" required>
          <el-date-picker
            v-model="dailyExecuteForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dailyExecuteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmDailyExecute">确认执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Calendar } from '@element-plus/icons-vue'
import { 
  getReconciliationTaskList, 
  executeReconciliation, 
  executeDailyReconciliation,
  retryReconciliationTask
} from '@/api/reconciliation'
import type { ReconciliationTask, ReconciliationQueryParams, ExecuteReconciliationRequest } from '@/api/reconciliation'

const router = useRouter()
const loading = ref(false)
const executeDialogVisible = ref(false)
const dailyExecuteDialogVisible = ref(false)
const dateRange = ref<[string, string] | null>(null)

const searchForm = reactive<ReconciliationQueryParams>({
  channel: '',
  status: '',
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

const tableData = ref<ReconciliationTask[]>([])

const executeForm = reactive<ExecuteReconciliationRequest>({
  channel: '',
  reconciliationDate: '',
  statementFileUrl: ''
})

const dailyExecuteForm = reactive({
  channel: '',
  date: ''
})

// 加载对账任务列表
const loadReconciliationList = async () => {
  loading.value = true
  try {
    const params: ReconciliationQueryParams = {
      ...searchForm,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getReconciliationTaskList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载对账任务列表失败:', error)
    ElMessage.warning('加载对账任务列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        taskNo: 'REC20241218001',
        channel: 'ALIPAY',
        reconciliationDate: '2024-12-17',
        status: 'COMPLETED',
        totalCount: 150,
        matchedCount: 148,
        unmatchedCount: 0,
        exceptionCount: 2,
        totalAmount: 12560.50,
        matchedAmount: 12450.00,
        unmatchedAmount: 110.50,
        startTime: '2024-12-18 02:00:00',
        endTime: '2024-12-18 02:05:30',
        duration: 330,
        tenantId: 1,
        createTime: '2024-12-18 02:00:00'
      },
      {
        id: 2,
        taskNo: 'REC20241218002',
        channel: 'WECHAT',
        reconciliationDate: '2024-12-17',
        status: 'COMPLETED',
        totalCount: 200,
        matchedCount: 200,
        unmatchedCount: 0,
        exceptionCount: 0,
        totalAmount: 18900.00,
        matchedAmount: 18900.00,
        unmatchedAmount: 0,
        startTime: '2024-12-18 02:10:00',
        endTime: '2024-12-18 02:14:25',
        duration: 265,
        tenantId: 1,
        createTime: '2024-12-18 02:10:00'
      }
    ]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const getChannelText = (channel: string) => {
  const channelMap: Record<string, string> = {
    ALIPAY: '支付宝',
    WECHAT: '微信支付'
  }
  return channelMap[channel] || channel
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '待执行',
    RUNNING: '执行中',
    COMPLETED: '已完成',
    FAILED: '失败'
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
  loadReconciliationList()
}

const handleReset = () => {
  searchForm.channel = ''
  searchForm.status = ''
  searchForm.startDate = ''
  searchForm.endDate = ''
  dateRange.value = null
  loadReconciliationList()
}

const handleExecute = () => {
  executeForm.channel = ''
  executeForm.reconciliationDate = ''
  executeForm.statementFileUrl = ''
  executeDialogVisible.value = true
}

const handleConfirmExecute = async () => {
  if (!executeForm.channel || !executeForm.reconciliationDate) {
    ElMessage.warning('请选择支付渠道和对账日期')
    return
  }

  try {
    const response = await executeReconciliation(executeForm)
    if (response.data) {
      ElMessage.success(`对账任务已创建：${response.data.taskNo}`)
      executeDialogVisible.value = false
      loadReconciliationList()
    }
  } catch (error) {
    console.error('执行对账失败:', error)
    ElMessage.error('执行对账失败')
  }
}

const handleDailyExecute = () => {
  dailyExecuteForm.channel = ''
  dailyExecuteForm.date = ''
  dailyExecuteDialogVisible.value = true
}

const handleConfirmDailyExecute = async () => {
  if (!dailyExecuteForm.channel || !dailyExecuteForm.date) {
    ElMessage.warning('请选择支付渠道和对账日期')
    return
  }

  try {
    const response = await executeDailyReconciliation(dailyExecuteForm.channel, dailyExecuteForm.date)
    if (response.data) {
      ElMessage.success(`每日对账任务已创建：${response.data.taskNo}`)
      dailyExecuteDialogVisible.value = false
      loadReconciliationList()
    }
  } catch (error) {
    console.error('执行每日对账失败:', error)
    ElMessage.error('执行每日对账失败')
  }
}

const handleViewReport = (row: ReconciliationTask) => {
  router.push(`/reconciliation/report/${row.taskNo}`)
}

const handleViewExceptions = (row: ReconciliationTask) => {
  router.push(`/reconciliation/exceptions?taskId=${row.id}`)
}

const handleRetry = async (row: ReconciliationTask) => {
  try {
    await ElMessageBox.confirm(
      `确认重试对账任务：${row.taskNo}？`,
      '确认重试',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await retryReconciliationTask(row.id)
    if (response.data) {
      ElMessage.success('对账任务重试成功')
      loadReconciliationList()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('重试对账任务失败:', error)
      ElMessage.error('重试对账任务失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadReconciliationList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadReconciliationList()
}

// 页面加载时获取数据
onMounted(() => {
  loadReconciliationList()
})
</script>

<style scoped>
.reconciliation-list {
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
