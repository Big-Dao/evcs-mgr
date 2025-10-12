<template>
  <div class="tenant-detail">
    <el-page-header @back="handleBack" content="租户详情">
      <template #extra>
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
      </template>
    </el-page-header>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-tag :type="getTenantTypeTag(detail.tenantType)">
            {{ getTenantTypeName(detail.tenantType) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="租户ID">{{ detail.tenantId }}</el-descriptions-item>
        <el-descriptions-item label="租户编码">{{ detail.tenantCode }}</el-descriptions-item>
        <el-descriptions-item label="租户名称">{{ detail.tenantName }}</el-descriptions-item>
        <el-descriptions-item label="租户类型">{{ getTenantTypeName(detail.tenantType) }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ detail.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>统计信息</span>
      </template>

      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="充电站数量" :value="detail.stationCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="充电桩数量" :value="detail.chargerCount">
            <template #suffix>个</template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="用户数量" :value="detail.userCount">
            <template #suffix>人</template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="订单总数" :value="detail.orderCount">
            <template #suffix>笔</template>
          </el-statistic>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>子租户列表</span>
      </template>

      <el-table :data="subTenants" style="width: 100%">
        <el-table-column prop="tenantCode" label="租户编码" width="120" />
        <el-table-column prop="tenantName" label="租户名称" />
        <el-table-column prop="tenantType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getTenantTypeName(row.tenantType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const detail = ref({
  tenantId: 1,
  tenantCode: 'T001',
  tenantName: '总部',
  tenantType: 'PLATFORM',
  contactName: '张三',
  contactPhone: '13800138000',
  contactEmail: 'zhangsan@example.com',
  status: 1,
  createTime: '2024-01-01 10:00:00',
  updateTime: '2024-10-11 15:30:00',
  stationCount: 45,
  chargerCount: 256,
  userCount: 128,
  orderCount: 15680
})

const subTenants = ref([
  {
    tenantCode: 'T002',
    tenantName: '华东运营商',
    tenantType: 'OPERATOR',
    status: 1
  },
  {
    tenantCode: 'T003',
    tenantName: '华南运营商',
    tenantType: 'OPERATOR',
    status: 1
  }
])

const getTenantTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    PLATFORM: '平台方',
    OPERATOR: '运营商',
    STATION: '站点方'
  }
  return typeMap[type] || type
}

const getTenantTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    PLATFORM: 'danger',
    OPERATOR: 'warning',
    STATION: 'success'
  }
  return tagMap[type] || ''
}

const handleBack = () => {
  router.back()
}

const handleEdit = () => {
  ElMessage.info('编辑租户功能')
}
</script>

<style scoped>
.tenant-detail {
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
</style>
