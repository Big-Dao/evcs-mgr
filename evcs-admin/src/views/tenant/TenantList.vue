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
          <el-input v-model="searchForm.tenantName" placeholder="请输入租户名称" clearable />
        </el-form-item>
        <el-form-item label="租户编码">
          <el-input v-model="searchForm.tenantCode" placeholder="请输入租户编码" clearable />
        </el-form-item>
        <el-form-item label="租户类型">
          <el-select v-model="searchForm.tenantType" placeholder="请选择" clearable>
            <el-option
              v-for="option in tenantTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="租户ID" width="90" />
        <el-table-column prop="tenantCode" label="租户编码" width="140" />
        <el-table-column prop="tenantName" label="租户名称" min-width="160" />
        <el-table-column prop="parentName" label="上级租户" width="160">
          <template #default="{ row }">
            {{ row.parentName || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="tenantTypeName" label="租户类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTenantTypeTag(row.tenantType)">
              {{ row.tenantTypeName || getTenantTypeName(row.tenantType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="childrenCount" label="子租户数" width="100">
          <template #default="{ row }">
            {{ row.childrenCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="租户编码" prop="tenantCode">
              <el-input v-model="formData.tenantCode" placeholder="请输入租户编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户名称" prop="tenantName">
              <el-input v-model="formData.tenantName" placeholder="请输入租户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级租户" prop="parentId">
              <el-tree-select
                v-model="formData.parentId"
                :data="filteredParentOptions"
                :props="treeSelectProps"
                :loading="parentLoading"
                check-strictly
                filterable
                clearable
                placeholder="请选择上级租户（默认顶级）"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="租户类型" prop="tenantType">
              <el-select v-model="formData.tenantType" placeholder="请选择租户类型" clearable>
                <el-option
                  v-for="option in tenantTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态">
                <el-option
                  v-for="option in statusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="formData.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系地址" prop="address">
              <el-input v-model="formData.address" placeholder="请输入联系地址" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大用户数" prop="maxUsers">
              <el-input-number
                v-model="formData.maxUsers"
                :min="0"
                :controls="false"
                placeholder="不限"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大站点数" prop="maxStations">
              <el-input-number
                v-model="formData.maxStations"
                :min="0"
                :controls="false"
                placeholder="不限"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大充电桩数" prop="maxChargers">
              <el-input-number
                v-model="formData.maxChargers"
                :min="0"
                :controls="false"
                placeholder="不限"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期时间" prop="expireTime">
              <el-date-picker
                v-model="formData.expireTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择过期时间"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="3"
                placeholder="请输入备注信息"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createTenant,
  deleteTenant,
  getTenantList,
  getTenantTree,
  updateTenant
} from '@/api/tenant'
import type { Tenant, TenantForm, TenantQueryParams } from '@/api/tenant'

interface TenantSelectNode {
  id: number
  tenantName: string
  parentId?: number
  children?: TenantSelectNode[]
}

interface TenantFormState {
  id?: number
  tenantCode: string
  tenantName: string
  tenantType?: number
  parentId?: number
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  status: number
  maxUsers?: number
  maxStations?: number
  maxChargers?: number
  expireTime?: string | null
  remark?: string | null
}

const tenantTypeOptions = [
  { label: '平台方', value: 1 },
  { label: '运营商', value: 2 },
  { label: '站点方', value: 3 }
] as const

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
] as const

const TENANT_TYPE_NAME: Record<number, string> = {
  1: '平台方',
  2: '运营商',
  3: '站点方'
}

const TENANT_TYPE_TAG: Record<number, string> = {
  1: 'danger',
  2: 'warning',
  3: 'success'
}

const router = useRouter()
const loading = ref(false)
const parentLoading = ref(false)

const searchForm = reactive<TenantQueryParams>({
  tenantName: '',
  tenantCode: '',
  tenantType: undefined,
  status: undefined
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<Tenant[]>([])
const tenantTreeOptions = ref<TenantSelectNode[]>([])

const treeSelectProps = {
  value: 'id',
  label: 'tenantName',
  children: 'children'
} as const

const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const formRef = ref<FormInstance>()

const formData = reactive<TenantFormState>({
  id: undefined,
  tenantCode: '',
  tenantName: '',
  tenantType: undefined,
  parentId: undefined,
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  status: 1,
  maxUsers: undefined,
  maxStations: undefined,
  maxChargers: undefined,
  expireTime: null,
  remark: ''
})

const filteredParentOptions = computed(() =>
  filterTenantTree(tenantTreeOptions.value, formData.id)
)

const phoneValidator = (_: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback()
    return
  }
  const pattern = /^1[3-9]\d{9}$/
  if (!pattern.test(value)) {
    callback(new Error('手机号格式不正确'))
    return
  }
  callback()
}

const rules: FormRules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  tenantType: [{ required: true, message: '请选择租户类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择租户状态', trigger: 'change' }],
  contactPhone: [{ validator: phoneValidator, trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const normalizeTenantTypeValue = (type: number | string | undefined): number | undefined => {
  if (typeof type === 'number') {
    return type
  }
  if (typeof type === 'string' && type.length > 0) {
    const parsed = Number(type)
    if (!Number.isNaN(parsed)) {
      return parsed
    }
  }
  return undefined
}

const buildTenantTree = (tenants: Tenant[]): TenantSelectNode[] => {
  if (!Array.isArray(tenants)) {
    return []
  }

  const hasChildren = tenants.some(item => Array.isArray(item.children) && item.children.length > 0)
  if (hasChildren) {
    const convert = (nodes: Tenant[]): TenantSelectNode[] =>
      nodes.map(node => ({
        id: node.id,
        tenantName: node.tenantName,
        parentId: node.parentId,
        children: node.children && node.children.length > 0 ? convert(node.children) : undefined
      }))
    return convert(tenants)
  }

  const map = new Map<number, TenantSelectNode>()
  const roots: TenantSelectNode[] = []

  tenants.forEach(tenant => {
    map.set(tenant.id, {
      id: tenant.id,
      tenantName: tenant.tenantName,
      parentId: tenant.parentId,
      children: []
    })
  })

  tenants.forEach(tenant => {
    const parentId = tenant.parentId
    const node = map.get(tenant.id)
    if (!node) {
      return
    }
    if (parentId && map.has(parentId)) {
      map.get(parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })

  const prune = (nodes: TenantSelectNode[]): TenantSelectNode[] =>
    nodes.map(node => ({
      ...node,
      children: node.children && node.children.length > 0 ? prune(node.children) : undefined
    }))

  return prune(roots)
}

function filterTenantTree(nodes: TenantSelectNode[], excludeId?: number): TenantSelectNode[] {
  if (!excludeId) {
    return nodes
  }
  return nodes
    .filter(node => node.id !== excludeId)
    .map(node => {
      const children = node.children ? filterTenantTree(node.children, excludeId) : undefined
      return {
        ...node,
        children: children && children.length > 0 ? children : undefined
      }
    })
}

const cleanupNullableString = (value?: string | null): string | null => {
  const trimmed = value?.trim()
  return trimmed && trimmed.length > 0 ? trimmed : null
}

const cleanupOptionalString = (value?: string | null): string | undefined => {
  const trimmed = value?.trim()
  return trimmed && trimmed.length > 0 ? trimmed : undefined
}

const loadTenantOptions = async () => {
  parentLoading.value = true
  try {
    const response = await getTenantTree()
    const tenants = Array.isArray(response.data) ? (response.data as Tenant[]) : []
    tenantTreeOptions.value = buildTenantTree(tenants)
  } catch (error) {
    console.error('获取租户树失败:', error)
    tenantTreeOptions.value = []
  } finally {
    parentLoading.value = false
  }
}

const loadTenantList = async () => {
  loading.value = true
  try {
    const params: TenantQueryParams = {
      tenantName: cleanupOptionalString(searchForm.tenantName),
      tenantCode: cleanupOptionalString(searchForm.tenantCode),
      tenantType: searchForm.tenantType,
      status: searchForm.status,
      page: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getTenantList(params)
    if (response.data) {
      const records = response.data.records ?? []
      tableData.value = records.map(record => ({
        ...record,
        tenantType: normalizeTenantTypeValue(record.tenantType) ?? 0
      }))
      pagination.total = response.data.total ?? 0
    }
  } catch (error) {
    console.error('获取租户列表失败:', error)
    ElMessage.warning('获取租户列表失败，已加载示例数据')
    tableData.value = [
      {
        id: 1,
        tenantCode: 'T001',
        tenantName: '总部',
        tenantType: 1,
        tenantTypeName: '平台方',
        parentName: '—',
        childrenCount: 2,
        contactName: '张三',
        contactPhone: '13800138000',
        contactEmail: 'admin@example.com',
        address: '上海市浦东新区世纪大道',
        status: 1,
        maxUsers: 200,
        maxStations: 50,
        maxChargers: 500,
        expireTime: '2030-12-31 23:59:59',
        remark: '演示数据'
      },
      {
        id: 2,
        tenantCode: 'T002',
        tenantName: '华东运营商',
        tenantType: 2,
        tenantTypeName: '运营商',
        parentId: 1,
        parentName: '总部',
        childrenCount: 5,
        contactName: '李四',
        contactPhone: '13800138001',
        contactEmail: 'ops@example.com',
        address: '江苏省南京市鼓楼区',
        status: 1,
        maxUsers: 80,
        maxStations: 20,
        maxChargers: 160,
        expireTime: '2028-06-30 23:59:59',
        remark: '演示数据'
      }
    ] as Tenant[]
    pagination.total = tableData.value.length
  } finally {
    loading.value = false
  }
}

const resetFormData = (source?: Tenant) => {
  formData.id = source?.id
  formData.tenantCode = source?.tenantCode ?? ''
  formData.tenantName = source?.tenantName ?? ''
  const normalizedParentId = source?.parentId && source.parentId > 0 ? source.parentId : undefined
  formData.parentId = normalizedParentId
  formData.tenantType = normalizeTenantTypeValue(source?.tenantType)
  formData.contactName = source?.contactName ?? ''
  formData.contactPhone = source?.contactPhone ?? ''
  formData.contactEmail = source?.contactEmail ?? ''
  formData.address = source?.address ?? ''
  formData.status = source?.status ?? 1
  formData.maxUsers = typeof source?.maxUsers === 'number' ? source?.maxUsers : undefined
  formData.maxStations = typeof source?.maxStations === 'number' ? source?.maxStations : undefined
  formData.maxChargers = typeof source?.maxChargers === 'number' ? source?.maxChargers : undefined
  formData.expireTime = source?.expireTime ?? null
  formData.remark = (source?.remark ?? '') || ''
  formRef.value?.clearValidate()
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadTenantList()
}

const handleReset = () => {
  searchForm.tenantName = ''
  searchForm.tenantCode = ''
  searchForm.tenantType = undefined
  searchForm.status = undefined
  pagination.currentPage = 1
  loadTenantList()
}

const handleAdd = () => {
  dialogTitle.value = '新增租户'
  resetFormData()
  if (!tenantTreeOptions.value.length) {
    void loadTenantOptions()
  }
  dialogVisible.value = true
}

const handleView = (row: Tenant) => {
  router.push(`/tenants/${row.id}`)
}

const handleEdit = (row: Tenant) => {
  dialogTitle.value = '编辑租户'
  resetFormData(row)
  void loadTenantOptions()
  dialogVisible.value = true
}

const handleDelete = async (row: Tenant) => {
  try {
    await ElMessageBox.confirm(`确定要删除租户「${row.tenantName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTenant(row.id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && pagination.currentPage > 1) {
      pagination.currentPage -= 1
    }
    await loadTenantList()
    await loadTenantOptions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除租户失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const buildPayload = (): TenantForm => {
  if (!formData.tenantType) {
    throw new Error('请选择租户类型')
  }
  return {
    tenantCode: formData.tenantCode.trim(),
    tenantName: formData.tenantName.trim(),
    tenantType: formData.tenantType,
    parentId: formData.parentId,
    contactName: cleanupOptionalString(formData.contactName),
    contactPhone: cleanupOptionalString(formData.contactPhone),
    contactEmail: cleanupOptionalString(formData.contactEmail),
    address: cleanupOptionalString(formData.address),
    status: formData.status,
    maxUsers: typeof formData.maxUsers === 'number' ? formData.maxUsers : null,
    maxStations: typeof formData.maxStations === 'number' ? formData.maxStations : null,
    maxChargers: typeof formData.maxChargers === 'number' ? formData.maxChargers : null,
    expireTime: formData.expireTime || null,
    remark: cleanupNullableString(formData.remark)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) {
    return
  }
  try {
    const valid = await formRef.value.validate()
    if (!valid) {
      return
    }

    let payload: TenantForm
    try {
      payload = buildPayload()
    } catch (error) {
      ElMessage.warning((error as Error).message)
      return
    }

    if (formData.id) {
      await updateTenant(formData.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createTenant(payload)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    await loadTenantList()
    await loadTenantOptions()
  } catch (error) {
    console.error('保存租户失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadTenantList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadTenantList()
}

const getTenantTypeName = (type: number | string | undefined) => {
  const normalized = normalizeTenantTypeValue(type)
  if (normalized && TENANT_TYPE_NAME[normalized]) {
    return TENANT_TYPE_NAME[normalized]
  }
  return typeof type === 'string' ? type : ''
}

const getTenantTypeTag = (type: number | string | undefined) => {
  const normalized = normalizeTenantTypeValue(type)
  if (normalized && TENANT_TYPE_TAG[normalized]) {
    return TENANT_TYPE_TAG[normalized]
  }
  return 'info'
}

onMounted(() => {
  void loadTenantOptions()
  void loadTenantList()
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
