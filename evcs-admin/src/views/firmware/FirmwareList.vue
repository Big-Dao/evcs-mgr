<template>
  <div class="firmware-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>固件管理</span>
          <el-button type="primary" @click="handleCreate">上传固件</el-button>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="firmwareVersion" label="版本号" width="120" />
        <el-table-column prop="model" label="适用型号" width="150" />
        <el-table-column prop="url" label="下载地址" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" type="success" @click="handleUpgrade(scope.row)">发起升级</el-button>
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

    <!-- 上传对话框 -->
    <el-dialog v-model="dialogVisible" title="上传固件" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="版本号">
          <el-input v-model="form.firmwareVersion" />
        </el-form-item>
        <el-form-item label="适用型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="下载地址">
          <el-input v-model="form.url" />
        </el-form-item>
        <el-form-item label="MD5校验">
          <el-input v-model="form.md5" />
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

    <!-- 升级对话框 -->
    <el-dialog v-model="upgradeDialogVisible" title="发起升级" width="400px">
      <el-form label-width="100px">
        <el-form-item label="充电桩ID">
          <el-input v-model="upgradeChargerId" placeholder="请输入目标充电桩ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="upgradeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUpgrade">开始升级</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { listFirmware, uploadFirmware, createUpgradeTask, type Firmware } from '@/api/firmware'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref<Firmware[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const upgradeDialogVisible = ref(false)
const currentFirmwareId = ref<number | null>(null)
const upgradeChargerId = ref('')

const queryParams = reactive({
  page: 1,
  size: 10
})

const form = reactive<Firmware>({
  firmwareVersion: '',
  model: '',
  url: '',
  md5: '',
  description: ''
})

const getList = async () => {
  loading.value = true
  try {
    const res = await listFirmware(queryParams)
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
  form.firmwareVersion = ''
  form.model = ''
  form.url = ''
  form.md5 = ''
  form.description = ''
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    const res = await uploadFirmware(form)
    if (res.code === 200) {
      ElMessage.success('上传成功')
      dialogVisible.value = false
      getList()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    console.error(error)
  }
}

const handleUpgrade = (row: Firmware) => {
  currentFirmwareId.value = row.id!
  upgradeChargerId.value = ''
  upgradeDialogVisible.value = true
}

const submitUpgrade = async () => {
  if (!upgradeChargerId.value) {
    ElMessage.warning('请输入充电桩ID')
    return
  }
  try {
    const res = await createUpgradeTask(currentFirmwareId.value!, Number(upgradeChargerId.value))
    if (res.code === 200) {
      ElMessage.success('升级任务已创建')
      upgradeDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '创建失败')
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
.firmware-container {
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
</style>
