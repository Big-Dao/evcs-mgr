<template>
  <div class="payment-detail">
    <el-page-header @back="handleBack" content="支付详情">
      <template #extra>
        <el-button 
          v-if="detail.status === 'SUCCESS' && detail.refundAmount === 0" 
          type="warning" 
          @click="handleRefund"
        >
          <el-icon><RefreshLeft /></el-icon>
          申请退款
        </el-button>
        <el-button type="primary" @click="handleQuery">
          <el-icon><Search /></el-icon>
          查询最新状态
        </el-button>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>支付信息</span>
              <el-tag :type="getStatusType(detail.status)" size="large">
                {{ getStatusText(detail.status) }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="交易号">{{ detail.tradeNo }}</el-descriptions-item>
            <el-descriptions-item label="订单号">
              <el-link type="primary" @click="handleViewOrder">{{ detail.orderNo }}</el-link>
            </el-descriptions-item>
            <el-descriptions-item label="支付渠道">{{ getChannelText(detail.channel) }}</el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ detail.paymentMethod }}</el-descriptions-item>
            <el-descriptions-item label="总金额">
              <span style="color: #409eff; font-weight: bold;">¥{{ detail.totalAmount?.toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span style="color: #67c23a; font-weight: bold;">¥{{ detail.paidAmount?.toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="退款金额">
              <span v-if="detail.refundAmount > 0" style="color: #f56c6c; font-weight: bold;">
                ¥{{ detail.refundAmount.toFixed(2) }}
              </span>
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="getStatusType(detail.status)">
                {{ getStatusText(detail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="detail-card" style="margin-top: 20px;" v-if="detail.channelTradeNo">
          <template #header>
            <span>渠道信息</span>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="渠道交易号">{{ detail.channelTradeNo }}</el-descriptions-item>
            <el-descriptions-item label="买家ID" v-if="detail.buyerId">{{ detail.buyerId }}</el-descriptions-item>
            <el-descriptions-item label="买家账号" v-if="detail.buyerAccount">{{ detail.buyerAccount }}</el-descriptions-item>
            <el-descriptions-item label="交易标题">{{ detail.subject }}</el-descriptions-item>
            <el-descriptions-item label="交易描述" v-if="detail.body">{{ detail.body }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>用户信息</span>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
            <el-descriptions-item label="用户名称" v-if="detail.userName">{{ detail.userName }}</el-descriptions-item>
            <el-descriptions-item label="租户ID">{{ detail.tenantId }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card style="margin-top: 20px;" v-if="detail.errorCode || detail.errorMsg">
          <template #header>
            <span style="color: #f56c6c;">错误信息</span>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="错误代码">{{ detail.errorCode }}</el-descriptions-item>
            <el-descriptions-item label="错误描述">{{ detail.errorMsg }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>操作日志</span>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="log in operationLogs"
              :key="log.id"
              :timestamp="log.timestamp"
              :type="log.type"
            >
              {{ log.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 退款对话框 -->
    <el-dialog v-model="refundDialogVisible" title="申请退款" width="500px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="交易号">
          <el-input v-model="refundForm.tradeNo" disabled />
        </el-form-item>
        <el-form-item label="支付金额">
          <el-input :value="'¥' + detail.paidAmount?.toFixed(2)" disabled />
        </el-form-item>
        <el-form-item label="退款金额" required>
          <el-input-number
            v-model="refundForm.refundAmount"
            :min="0.01"
            :max="detail.paidAmount"
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
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import { queryPayment, refundPayment } from '@/api/payment'
import type { PaymentDetail, RefundRequest } from '@/api/payment'

const router = useRouter()
const route = useRoute()
const refundDialogVisible = ref(false)

const detail = ref<PaymentDetail>({
  id: 0,
  tradeNo: '',
  orderNo: '',
  channel: '',
  paymentMethod: '',
  totalAmount: 0,
  paidAmount: 0,
  refundAmount: 0,
  status: '',
  userId: 0,
  tenantId: 0,
  createTime: ''
})

const refundForm = reactive<RefundRequest>({
  tradeNo: '',
  refundAmount: 0,
  refundReason: ''
})

const operationLogs = ref([
  {
    id: 1,
    timestamp: '2024-12-18 10:30:00',
    type: 'primary',
    content: '创建支付订单'
  },
  {
    id: 2,
    timestamp: '2024-12-18 10:30:05',
    type: 'success',
    content: '支付成功'
  }
])

// 加载支付详情
const loadPaymentDetail = async () => {
  try {
    const tradeNo = route.params.id as string
    const response = await queryPayment(tradeNo)
    if (response.data) {
      detail.value = response.data
    }
  } catch (error) {
    console.error('加载支付详情失败:', error)
    ElMessage.warning('加载支付详情失败，显示模拟数据')
    // Fallback to mock data
    detail.value = {
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
      transactionId: 'ALI2024121810300512345',
      channelTradeNo: '2024121822001234567890123',
      buyerId: '2088123456789012',
      buyerAccount: '138****5678',
      subject: '充电订单支付',
      body: '订单号：ORD20241218001',
      userId: 1,
      userName: '张三',
      tenantId: 1,
      createTime: '2024-12-18 10:30:00'
    }
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

const handleBack = () => {
  router.back()
}

const handleViewOrder = () => {
  // 跳转到订单详情页
  router.push(`/orders/${detail.value.orderNo}`)
}

const handleQuery = async () => {
  try {
    const response = await queryPayment(detail.value.tradeNo)
    if (response.data) {
      detail.value = response.data
      ElMessage.success('查询成功，状态已更新')
    }
  } catch (error) {
    console.error('查询支付失败:', error)
    ElMessage.error('查询支付失败')
  }
}

const handleRefund = () => {
  refundForm.tradeNo = detail.value.tradeNo
  refundForm.refundAmount = detail.value.paidAmount
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
      await loadPaymentDetail()
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

// 页面加载时获取数据
onMounted(() => {
  loadPaymentDetail()
})
</script>

<style scoped>
.payment-detail {
  height: 100%;
  padding: 20px;
}

.detail-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
