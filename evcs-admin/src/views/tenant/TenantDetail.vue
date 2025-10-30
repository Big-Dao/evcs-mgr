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
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTenantDetail, getTenantTree } from '@/api/tenant'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const detail = ref({
  tenantId: 0,
  tenantCode: '',
  tenantName: '',
  tenantType: 1,
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  status: 1,
  createTime: '',
  updateTime: '',
  stationCount: 0,
  chargerCount: 0,
  userCount: 0,
  orderCount: 0
})

const subTenants = ref<any[]>([])

// 加载租户详情
const loadTenantDetail = async () => {
  try {
    loading.value = true
    const tenantId = Number(route.params.id || route.query.id)
    if (!tenantId) {
      ElMessage.error('租户ID不能为空')
      return
    }

    const response = await getTenantDetail(tenantId)
    if (response.code === 200 && response.data) {
      const data: any = response.data
      detail.value = {
        tenantId: data.tenantId || data.id,
        tenantCode: data.tenantCode || '',
        tenantName: data.tenantName || '',
        tenantType: data.tenantType || 1,
        contactName: data.contactPerson || data.contactName || '',
        contactPhone: data.contactPhone || '',
        contactEmail: data.contactEmail || '',
        status: data.status || 1,
        createTime: data.createTime || '',
        updateTime: data.updateTime || '',
        stationCount: data.stationCount || 0,
        chargerCount: data.chargerCount || 0,
        userCount: data.userCount || 0,
        orderCount: data.orderCount || 0
      }
      
      // 加载子租户
      await loadSubTenants(tenantId)
    } else {
      ElMessage.error(response.message || '加载租户详情失败')
    }
  } catch (error) {
    console.error('加载租户详情失败:', error)
    ElMessage.error('加载租户详情失败')
  } finally {
    loading.value = false
  }
}

// 加载子租户列表
const loadSubTenants = async (parentId: number) => {
  try {
    const response = await getTenantTree()
    if (response.code === 200 && response.data) {
      // 过滤出当前租户的直接子租户
      const tenants = Array.isArray(response.data) ? response.data : []
      subTenants.value = tenants.filter((t: any) => t.parentId === parentId)
    }
  } catch (error) {
    console.error('加载子租户失败:', error)
  }
}

onMounted(() => {
  loadTenantDetail()
})

const getTenantTypeName = (type: number) => {
  const typeMap: Record<number, string> = {
    1: '平台方',
    2: '运营商'
  }
  return typeMap[type] || '未知'
}

const getTenantTypeTag = (type: number) => {
  const tagMap: Record<number, string> = {
    1: 'danger',
    2: 'warning'
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
