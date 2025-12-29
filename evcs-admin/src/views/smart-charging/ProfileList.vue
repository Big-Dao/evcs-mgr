<template>
  <div class="smart-charging-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>智能充电策略</span>
          <el-button type="primary" @click="handleCreate">新建策略</el-button>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="chargerId" label="充电桩ID" width="100" />
        <el-table-column prop="connectorId" label="枪口" width="80" />
        <el-table-column prop="purpose" label="目的" width="150" />
        <el-table-column prop="kind" label="类型" width="120" />
        <el-table-column prop="limitKw" label="功率限制(kW)" width="120" />
        <el-table-column prop="stackLevel" label="优先级" width="80" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleApply(scope.row)">下发策略</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 创建对话框 -->
    <el-dialog v-model="dialogVisible" title="新建策略" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="充电桩ID">
          <el-input v-model.number="form.chargerId" />
        </el-form-item>
        <el-form-item label="枪口号">
          <el-input-number v-model="form.connectorId" :min="0" />
          <span class="tips">0表示整桩</span>
        </el-form-item>
        <el-form-item label="功率限制(kW)">
          <el-input-number v-model="form.limitKw" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.stackLevel" :min="0" />
        </el-form-item>
        <el-form-item label="策略目的">
          <el-select v-model="form.purpose">
            <el-option label="TxDefaultProfile" value="TxDefaultProfile" />
            <el-option label="TxProfile" value="TxProfile" />
            <el-option label="ChargePointMaxProfile" value="ChargePointMaxProfile" />
          </el-select>
        </el-form-item>
        <el-form-item label="策略类型">
          <el-select v-model="form.kind">
            <el-option label="Absolute" value="Absolute" />
            <el-option label="Recurring" value="Recurring" />
            <el-option label="Relative" value="Relative" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="form.description" />
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
import { ref, reactive, onMounted } from 'vue'
import { listProfiles, createProfile, applyProfile, type ChargingProfile } from '@/api/smart-charging'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref<ChargingProfile[]>([])
const total = ref(0)
const dialogVisible = ref(false)

const queryParams = reactive({
  page: 1,
  size: 10
})

const form = reactive<ChargingProfile>({
  chargerId: 0,
  connectorId: 0,
  stackLevel: 0,
  purpose: 'TxDefaultProfile',
  kind: 'Absolute',
  limitKw: 0,
  description: ''
})

const getList = async () => {
  loading.value = true
  try {
    const res = await listProfiles(queryParams)
    if (res.code === 200) {
      const data = res.data as any
      tableData.value = data.records
      total.value = data.total
    }
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  form.chargerId = 0
  form.connectorId = 0
  form.stackLevel = 0
  form.limitKw = 0
  form.description = ''
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    const res = await createProfile(form)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      getList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    console.error(error)
  }
}

const handleApply = async (row: ChargingProfile) => {
  try {
    const res = await applyProfile(row.id!)
    if (res.code === 200) {
      ElMessage.success('下发指令已发送')
    } else {
      ElMessage.error(res.message || '下发失败')
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.smart-charging-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.tips {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
