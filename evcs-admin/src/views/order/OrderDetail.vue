<template>
  <div class="order-detail">
    <el-page-header @back="handleBack" content="订单详情">
      <template #extra>
        <el-button v-if="detail.status === 'charging'" type="danger" @click="handleStopCharge">
          <el-icon><VideoPause /></el-icon>
          停止充电
        </el-button>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>
          导出订单
        </el-button>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>订单信息</span>
              <el-tag :type="getStatusType(detail.status)" size="large">
                {{ getStatusText(detail.status) }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="充电站">{{ detail.stationName }}</el-descriptions-item>
            <el-descriptions-item label="充电桩">{{ detail.chargerCode }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ detail.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ detail.endTime || '进行中' }}</el-descriptions-item>
            <el-descriptions-item label="充电时长">{{ detail.duration }} 分钟</el-descriptions-item>
            <el-descriptions-item label="充电电量">{{ detail.energy }} kWh</el-descriptions-item>
            <el-descriptions-item label="订单金额">
              <span style="color: #f56c6c; font-size: 20px; font-weight: bold;">¥{{ detail.amount.toFixed(2) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="计费方案">{{ detail.billingPlan }}</el-descriptions-item>
            <el-descriptions-item label="用户信息">{{ detail.userName }} ({{ detail.userPhone }})</el-descriptions-item>
            <el-descriptions-item label="车牌号">{{ detail.vehiclePlate || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="detail-card" style="margin-top: 20px;">
          <template #header>
            <span>充电时间轴</span>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="event in chargingTimeline"
              :key="event.id"
              :timestamp="event.timestamp"
              :type="event.type"
              :icon="event.icon"
            >
              <h4>{{ event.title }}</h4>
              <p v-if="event.content" style="margin-top: 5px; color: #909399;">{{ event.content }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card class="detail-card" style="margin-top: 20px;">
          <template #header>
            <span>充电曲线</span>
          </template>

          <div style="height: 300px; background: #f5f5f5; display: flex; align-items: center; justify-content: center;">
            <div style="text-align: center; color: #999;">
              <el-icon :size="60"><TrendCharts /></el-icon>
              <p style="margin-top: 10px;">充电曲线图表（需集成 ECharts）</p>
              <p style="font-size: 14px;">显示电压、电流、功率、SOC等充电过程数据</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <span>支付信息</span>
          </template>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="支付状态">
              <el-tag :type="detail.paymentStatus === 'paid' ? 'success' : 'warning'">
                {{ detail.paymentStatus === 'paid' ? '已支付' : '待支付' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付方式">{{ detail.paymentMethod }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ detail.paymentTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付流水号">{{ detail.paymentSerial || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <div style="padding: 10px 0;">
            <div class="cost-item">
              <span>电费</span>
              <span>¥{{ detail.electricityCost.toFixed(2) }}</span>
            </div>
            <div class="cost-item">
              <span>服务费</span>
              <span>¥{{ detail.serviceCost.toFixed(2) }}</span>
            </div>
            <div class="cost-item" style="font-size: 18px; font-weight: bold; margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee;">
              <span>合计</span>
              <span style="color: #f56c6c;">¥{{ detail.amount.toFixed(2) }}</span>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>计费详情</span>
          </template>

          <div v-for="segment in billingSegments" :key="segment.id" class="billing-segment">
            <div class="segment-header">
              <span class="segment-time">{{ segment.startTime }} - {{ segment.endTime }}</span>
              <el-tag size="small" :type="segment.type">{{ segment.name }}</el-tag>
            </div>
            <div class="segment-body">
              <div class="segment-item">
                <span>电费单价</span>
                <span>¥{{ segment.electricityPrice }}/kWh</span>
              </div>
              <div class="segment-item">
                <span>服务费单价</span>
                <span>¥{{ segment.servicePrice }}/kWh</span>
              </div>
              <div class="segment-item">
                <span>充电电量</span>
                <span>{{ segment.energy }} kWh</span>
              </div>
              <div class="segment-item" style="font-weight: bold;">
                <span>小计</span>
                <span>¥{{ segment.subtotal.toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 20px;">
          <template #header>
            <span>操作记录</span>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="log in operationLogs"
              :key="log.id"
              :timestamp="log.timestamp"
              :type="log.type"
              size="small"
            >
              {{ log.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const detail = ref({
  orderId: 1,
  orderNo: 'ORD20241011001',
  stationName: '市中心充电站',
  chargerCode: 'CH001',
  startTime: '2024-10-11 10:30:00',
  endTime: '2024-10-11 12:00:00',
  duration: 90,
  energy: 45.8,
  amount: 68.70,
  electricityCost: 45.80,
  serviceCost: 22.90,
  status: 'completed',
  billingPlan: '标准分时计费方案',
  userName: '张三',
  userPhone: '138****8000',
  vehiclePlate: '浙A88888',
  paymentStatus: 'paid',
  paymentMethod: '微信支付',
  paymentTime: '2024-10-11 12:05:00',
  paymentSerial: 'WX20241011120500123456'
})

const chargingTimeline = ref([
  {
    id: 1,
    timestamp: '2024-10-11 12:00:00',
    type: 'success',
    icon: 'CircleCheck',
    title: '充电完成',
    content: '充电电量: 45.8 kWh, 充电时长: 90 分钟'
  },
  {
    id: 2,
    timestamp: '2024-10-11 11:30:00',
    type: 'primary',
    icon: 'Loading',
    title: '充电中',
    content: 'SOC已达80%, 充电功率: 45 kW'
  },
  {
    id: 3,
    timestamp: '2024-10-11 11:00:00',
    type: 'primary',
    icon: 'Loading',
    title: '充电中',
    content: 'SOC已达50%, 充电功率: 60 kW'
  },
  {
    id: 4,
    timestamp: '2024-10-11 10:30:00',
    type: 'success',
    icon: 'VideoPlay',
    title: '开始充电',
    content: '初始SOC: 15%, 充电功率: 120 kW'
  },
  {
    id: 5,
    timestamp: '2024-10-11 10:28:00',
    type: 'info',
    icon: 'Connection',
    title: '设备连接',
    content: '充电桩与车辆建立连接'
  },
  {
    id: 6,
    timestamp: '2024-10-11 10:25:00',
    type: 'info',
    icon: 'Document',
    title: '订单创建',
    content: '用户扫码创建订单'
  }
])

const billingSegments = ref([
  {
    id: 1,
    startTime: '10:30',
    endTime: '11:00',
    name: '平时段',
    type: 'primary',
    electricityPrice: 0.7,
    servicePrice: 0.3,
    energy: 15.0,
    subtotal: 15.0
  },
  {
    id: 2,
    startTime: '11:00',
    endTime: '12:00',
    name: '高峰时段',
    type: 'warning',
    electricityPrice: 0.9,
    servicePrice: 0.5,
    energy: 30.8,
    subtotal: 43.12
  }
])

const operationLogs = ref([
  {
    id: 1,
    timestamp: '2024-10-11 12:05:00',
    type: 'success',
    content: '用户支付成功'
  },
  {
    id: 2,
    timestamp: '2024-10-11 12:00:00',
    type: 'success',
    content: '充电完成'
  },
  {
    id: 3,
    timestamp: '2024-10-11 10:30:00',
    type: 'success',
    content: '充电启动'
  },
  {
    id: 4,
    timestamp: '2024-10-11 10:25:00',
    type: 'info',
    content: '订单创建'
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

const handleBack = () => {
  router.back()
}

const handleStopCharge = () => {
  ElMessageBox.confirm('确定要停止充电吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('充电已停止')
  })
}

const handleExport = () => {
  ElMessage.success('订单导出成功')
}
</script>

<style scoped>
.order-detail {
  height: 100%;
}

.detail-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cost-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.billing-segment {
  margin-bottom: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.segment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.segment-time {
  font-weight: bold;
  color: #303133;
}

.segment-body {
  padding-left: 10px;
}

.segment-item {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
  color: #606266;
}
</style>
