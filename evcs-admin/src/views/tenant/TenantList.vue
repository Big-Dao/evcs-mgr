<template>
  <div class="tenant-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增租户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="租户名称">
          <el-input v-model="searchForm.name" placeholder="请输入租户名称" clearable />
        </el-form-item>
        <el-form-item label="租户类型">
          <el-select v-model="searchForm.type" placeholder="请选择" clearable>
            <el-option label="平台方" value="PLATFORM" />
            <el-option label="运营商" value="OPERATOR" />
            <el-option label="站点方" value="STATION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="租户ID" width="80" />
        <el-table-column prop="tenantCode" label="租户编码" width="120" />
        <el-table-column prop="tenantName" label="租户名称" />
        <el-table-column prop="tenantType" label="租户类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTenantTypeTag(row.tenantType)">
              {{ getTenantTypeName(row.tenantType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="租户编码" prop="tenantCode">
          <el-input v-model="formData.tenantCode" placeholder="请输入租户编码" />
        </el-form-item>
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="formData.tenantName" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="租户类型" prop="tenantType">
          <el-select v-model="formData.tenantType" placeholder="请选择租户类型">
            <el-option label="平台方" value="PLATFORM" />
            <el-option label="运营商" value="OPERATOR" />
            <el-option label="站点方" value="STATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getTenantList, createTenant, updateTenant, deleteTenant } from '@/api/tenant'
import type { Tenant, TenantQueryParams } from '@/api/tenant'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive<TenantQueryParams>({
  name: '',
  type: '',
  status: undefined,
  current: 1,
  size: 10
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<Tenant[]>([])

// 加载租户列表
const loadTenantList = async () => {
  loading.value = true
  try {
    const params: TenantQueryParams = {
      name: searchForm.name,
      type: searchForm.type,
      status: searchForm.status,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getTenantList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载租户列表失败:', error)
    ElMessage.warning('加载租户列表失败，显示模拟数据')
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        tenantCode: 'T001',
        tenantName: '总部',
        tenantType: 'PLATFORM',
        contactName: '张三',
        contactPhone: '13800138000',
        status: 1
      },
      {
        id: 2,
        tenantCode: 'T002',
        tenantName: '华东运营商',
        tenantType: 'OPERATOR',
        contactName: '李四',
        contactPhone: '13800138001',
        status: 1
      }
    ] as Tenant[]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const formRef = ref<FormInstance>()

const formData = reactive({
  id: undefined as number | undefined,
  tenantCode: '',
  tenantName: '',
  tenantType: '',
  contactName: '',
  contactPhone: '',
  status: 1
})

const rules: FormRules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  tenantType: [{ required: true, message: '请选择租户类型', trigger: 'change' }]
}

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

const handleSearch = () => {
  pagination.currentPage = 1
  loadTenantList()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.type = ''
  searchForm.status = undefined
  loadTenantList()
}

const handleAdd = () => {
  dialogTitle.value = '新增租户'
  // Reset form data
  formData.id = undefined
  formData.tenantCode = ''
  formData.tenantName = ''
  formData.tenantType = ''
  formData.contactName = ''
  formData.contactPhone = ''
  formData.status = 1
  dialogVisible.value = true
}

const handleView = (row: Tenant) => {
  router.push(`/tenants/${row.id}`)
}

const handleEdit = (row: Tenant) => {
  dialogTitle.value = '编辑租户'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: Tenant) => {
  try {
    await ElMessageBox.confirm('确定要删除该租户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteTenant(row.id)
    ElMessage.success('删除成功')
    loadTenantList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
    
    if (formData.id) {
      await updateTenant(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createTenant(formData)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadTenantList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadTenantList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadTenantList()
}

// 页面加载时获取数据
onMounted(() => {
  loadTenantList()
})
</script>

<style scoped>
.tenant-list {
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
