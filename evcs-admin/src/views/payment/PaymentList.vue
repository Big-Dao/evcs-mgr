<template>
  <div class="payment-list" data-testid="payment-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span data-testid="payment-page-title">支付管理</span>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出数据
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="交易号">
          <el-input v-model="searchForm.tradeNo" placeholder="请输入交易号" clearable />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="支付渠道">
          <el-select v-model="searchForm.channel" placeholder="请选择" clearable>
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信支付" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="待支付" value="PENDING" />
            <el-option label="支付成功" value="SUCCESS" />
            <el-option label="支付失败" value="FAILED" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" data-testid="payment-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tradeNo" label="交易号" width="160" />
        <el-table-column prop="orderNo" label="订单号" width="140" />
        <el-table-column prop="channel" label="支付渠道" width="100">
          <template #default="{ row }">
            {{ getChannelText(row.channel) }}
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100" />
        <el-table-column prop="totalAmount" label="总金额(元)" width="100">
          <template #default="{ row }">
            ¥{{ row.totalAmount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="实付金额(元)" width="110">
          <template #default="{ row }">
            ¥{{ row.paidAmount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额(元)" width="110">
          <template #default="{ row }">
            <span v-if="row.refundAmount > 0" style="color: red;">
              ¥{{ row.refundAmount.toFixed(2) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button 
              v-if="row.status === 'SUCCESS' && row.refundAmount === 0" 
              link 
              type="warning" 
              size="small" 
              @click="handleRefund(row)"
            >
              退款
            </el-button>
            <el-button link type="info" size="small" @click="handleQuery(row)">查询</el-button>
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

    <!-- 退款对话框 -->
    <el-dialog v-model="refundDialogVisible" title="申请退款" width="500px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="交易号">
          <el-input v-model="refundForm.tradeNo" disabled />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="currentPayment.orderNo" disabled />
        </el-form-item>
        <el-form-item label="支付金额">
          <el-input :value="'¥' + currentPayment.paidAmount?.toFixed(2)" disabled />
        </el-form-item>
        <el-form-item label="退款金额" required>
          <el-input-number
            v-model="refundForm.refundAmount"
            :min="0.01"
            :max="currentPayment.paidAmount"
            :precision="2"
            :step="0.01"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-input
            v-model="refundForm.refundReason"
            type="textarea"
            :rows="3"
            placeholder="请输入退款原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getPaymentList, queryPayment, refundPayment } from '@/api/payment'
import type { Payment, PaymentQueryParams, RefundRequest } from '@/api/payment'

const router = useRouter()
const loading = ref(false)
const refundDialogVisible = ref(false)
const dateRange = ref<[string, string] | null>(null)

const searchForm = reactive<PaymentQueryParams>({
  tradeNo: '',
  orderNo: '',
  channel: '',
  status: '',
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

const tableData = ref<Payment[]>([])
const currentPayment = ref<Payment>({} as Payment)

const refundForm = reactive<RefundRequest>({
  tradeNo: '',
  refundAmount: 0,
  refundReason: ''
})

// 加载支付列表
const loadPaymentList = async () => {
  loading.value = true
  try {
    const params: PaymentQueryParams = {
      ...searchForm,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getPaymentList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载支付列表失败:', error)
    ElMessage.warning('加载支付列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        tradeNo: 'TRD20241218001',
        orderNo: 'ORD20241218001',
        channel: 'ALIPAY',
        paymentMethod: '扫码支付',
        totalAmount: 68.70,
        paidAmount: 68.70,
        refundAmount: 0,
        status: 'SUCCESS',
        payTime: '2024-12-18 10:30:05',
        userId: 1,
        tenantId: 1,
        createTime: '2024-12-18 10:30:00'
      },
      {
        id: 2,
        tradeNo: 'TRD20241218002',
        orderNo: 'ORD20241218002',
        channel: 'WECHAT',
        paymentMethod: '扫码支付',
        totalAmount: 88.50,
        paidAmount: 88.50,
        refundAmount: 0,
        status: 'SUCCESS',
        payTime: '2024-12-18 11:15:30',
        userId: 2,
        tenantId: 1,
        createTime: '2024-12-18 11:15:25'
      },
      {
        id: 3,
        tradeNo: 'TRD20241218003',
        orderNo: 'ORD20241218003',
        channel: 'ALIPAY',
        paymentMethod: '扫码支付',
        totalAmount: 45.00,
        paidAmount: 0,
        refundAmount: 0,
        status: 'PENDING',
        userId: 3,
        tenantId: 1,
        createTime: '2024-12-18 12:00:00'
      }
    ]
    pagination.total = 3
  } finally {
    loading.value = false
  }
}

const getChannelText = (channel: string) => {
  const channelMap: Record<string, string> = {
    ALIPAY: '支付宝',
    WECHAT: '微信支付',
    UNIONPAY: '银联'
  }
  return channelMap[channel] || channel
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    PENDING: 'info',
    SUCCESS: 'success',
    FAILED: 'danger',
    REFUNDED: 'warning'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '待支付',
    SUCCESS: '支付成功',
    FAILED: '支付失败',
    REFUNDED: '已退款'
  }
  return textMap[status] || status
}

const handleDateChange = (value: [string, string] | null) => {
  if (value) {
    searchForm.startTime = value[0]
    searchForm.endTime = value[1]
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadPaymentList()
}

const handleReset = () => {
  searchForm.tradeNo = ''
  searchForm.orderNo = ''
  searchForm.channel = ''
  searchForm.status = ''
  searchForm.startTime = ''
  searchForm.endTime = ''
  dateRange.value = null
  loadPaymentList()
}

const handleView = (row: Payment) => {
  router.push(`/payments/${row.id}`)
}

const handleQuery = async (row: Payment) => {
  try {
    const response = await queryPayment(row.tradeNo)
    if (response.data) {
      ElMessageBox.alert(
        `交易状态: ${getStatusText(response.data.status)}<br/>
         支付时间: ${response.data.payTime || '未支付'}<br/>
         支付金额: ¥${response.data.paidAmount.toFixed(2)}`,
        '支付查询结果',
        {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确定'
        }
      )
    }
  } catch (error) {
    console.error('查询支付失败:', error)
    ElMessage.error('查询支付失败')
  }
}

const handleRefund = (row: Payment) => {
  currentPayment.value = row
  refundForm.tradeNo = row.tradeNo
  refundForm.refundAmount = row.paidAmount
  refundForm.refundReason = ''
  refundDialogVisible.value = true
}

const handleConfirmRefund = async () => {
  if (!refundForm.refundReason) {
    ElMessage.warning('请输入退款原因')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认退款金额: ¥${refundForm.refundAmount.toFixed(2)}？`,
      '确认退款',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await refundPayment(refundForm)
    if (response.data?.success) {
      ElMessage.success('退款申请提交成功')
      refundDialogVisible.value = false
      loadPaymentList()
    } else {
      ElMessage.error(response.data?.message || '退款失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('退款失败:', error)
      ElMessage.error('退款失败')
    }
  }
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadPaymentList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadPaymentList()
}

// 页面加载时获取数据
onMounted(() => {
  loadPaymentList()
})
</script>

<style scoped>
.payment-list {
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
