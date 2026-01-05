<template>
  <div class="coupon-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>优惠券发放</span>
          <el-button type="primary" @click="dialogVisible = true">发放优惠券</el-button>
        </div>
      </template>

      <div class="filter-container">
        <el-input v-model="searchUserId" placeholder="输入用户ID查询" style="width: 200px;" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型">
          <template #default="scope">
            {{ scope.row.type === 1 ? '满减券' : '折扣券' }}
          </template>
        </el-table-column>
        <el-table-column prop="value" label="面值">
          <template #default="scope">
            {{ scope.row.type === 1 ? scope.row.value + '元' : (scope.row.value * 10) + '折' }}
          </template>
        </el-table-column>
        <el-table-column prop="minAmount" label="最低消费" />
        <el-table-column prop="startTime" label="开始时间" />
        <el-table-column prop="endTime" label="结束时间" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="发放优惠券" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="form.userId" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="面值">
          <el-input-number v-model="form.value" :precision="2" :step="0.1" />
          <span class="tips" v-if="form.type === 2">0.8表示8折</span>
        </el-form-item>
        <el-form-item label="最低消费">
          <el-input-number v-model="form.minAmount" :min="0" />
        </el-form-item>
        <el-form-item label="有效期(天)">
          <el-input-number v-model="form.validDays" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { issueCoupon, listMyCoupons, type Coupon } from '@/api/coupon'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref<Coupon[]>([])
const dialogVisible = ref(false)
const searchUserId = ref('')

const form = reactive({
  userId: '',
  name: '新用户专享券',
  type: 1,
  value: 10,
  minAmount: 20,
  validDays: 30
})

const handleSearch = async () => {
  if (!searchUserId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }
  loading.value = true
  try {
    const res = await listMyCoupons(Number(searchUserId.value))
    if (res.code === 200) {
      tableData.value = res.data as any
    }
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  try {
    const res = await issueCoupon(form)
    if (res.code === 200) {
      ElMessage.success('发放成功')
      dialogVisible.value = false
      if (searchUserId.value === form.userId) {
        handleSearch()
      }
    } else {
      ElMessage.error(res.message || '发放失败')
    }
  } catch (error) {
    console.error(error)
  }
}

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'success',
    1: 'info',
    2: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '未使用',
    1: '已使用',
    2: '已过期'
  }
  return map[status] || '未知'
}
</script>

<style scoped>
.coupon-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-container {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
.tips {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
