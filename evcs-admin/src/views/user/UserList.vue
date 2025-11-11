<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="登录账号">
          <el-input v-model="searchForm.loginIdentifier" placeholder="手机号 / 邮箱" clearable />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="loginIdentifier" label="登录账号" min-width="180" />
        <el-table-column prop="realName" label="真实姓名" min-width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="tenantName" label="所属租户" min-width="160">
          <template #default="{ row }">
            {{ row.tenantName || row.tenantId || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="登录账号" prop="loginIdentifier">
              <el-input v-model="formData.loginIdentifier" placeholder="手机号或邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!formData.id">
            <el-form-item label="初始密码" prop="password">
              <el-input
                v-model="formData.password"
                type="password"
                show-password
                placeholder="至少 6 位"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属租户" prop="tenantId">
              <el-tree-select
                v-model="formData.tenantId"
                :data="tenantTreeOptions"
                :props="treeSelectProps"
                :loading="tenantLoading"
                check-strictly
                placeholder="请选择所属租户"
                filterable
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户类型" prop="userType">
              <el-select v-model="formData.userType" placeholder="请选择用户类型" clearable>
                <el-option
                  v-for="option in userTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="formData.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择状态">
                <el-option :value="1" label="启用" />
                <el-option :value="0" label="停用" />
              </el-select>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createUser, deleteUser, getUserList, resetUserPassword, updateUser, type User, type UserForm, type UserQueryParams } from '@/api/user'
import { getTenantTree, type Tenant } from '@/api/tenant'

interface TenantNode {
  id: number
  tenantName: string
  parentId?: number
  children?: TenantNode[]
}

const router = useRouter()
const loading = ref(false)
const tenantLoading = ref(false)

const searchForm = reactive<UserQueryParams>({
  username: '',
  loginIdentifier: '',
  realName: '',
  current: 1,
  size: 10
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref<User[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref<FormInstance>()

const formData = reactive<User & { password?: string }>({
  id: 0,
  username: '',
  loginIdentifier: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  status: 1,
  userType: 2,
  tenantId: 0
})

const treeSelectProps = {
  label: 'tenantName',
  value: 'id',
  children: 'children'
} as const

const tenantTreeOptions = ref<TenantNode[]>([])

const userTypeOptions = [
  { label: '平台管理员', value: 1 },
  { label: '运营商用户', value: 2 },
  { label: '站点用户', value: 3 }
] as const

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  loginIdentifier: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [
    {
      required: true,
      trigger: 'blur',
      validator: (_rule, value, callback) => {
        if (formData.id) {
          callback()
        } else if (!value || value.length < 6) {
          callback(new Error('初始密码至少 6 位'))
        } else {
          callback()
        }
      }
    }
  ],
  tenantId: [{ required: true, message: '请选择所属租户', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [
    {
      trigger: 'blur',
      validator: (_rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        const pattern = /^1[3-9]\d{9}$/
        if (!pattern.test(value)) {
          callback(new Error('手机号格式不正确'))
        } else {
          callback()
        }
      }
    }
  ]
}

const normalizeTenantNodes = (tenantList: Tenant[]): TenantNode[] => {
  if (!Array.isArray(tenantList)) {
    return []
  }
  const hasChildren = tenantList.some(item => Array.isArray(item.children) && item.children.length > 0)
  if (hasChildren) {
    const convert = (nodes: Tenant[]): TenantNode[] =>
      nodes.map(node => ({
        id: node.id,
        tenantName: node.tenantName,
        parentId: node.parentId,
        children: node.children && node.children.length > 0 ? convert(node.children) : undefined
      }))
    return convert(tenantList)
  }

  const map = new Map<number, TenantNode>()
  const roots: TenantNode[] = []

  tenantList.forEach(t => {
    map.set(t.id, {
      id: t.id,
      tenantName: t.tenantName,
      parentId: t.parentId,
      children: []
    })
  })

  tenantList.forEach(t => {
    const node = map.get(t.id)!
    if (t.parentId && map.has(t.parentId)) {
      map.get(t.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })

  const prune = (nodes: TenantNode[]): TenantNode[] =>
    nodes.map(node => ({
      ...node,
      children: node.children && node.children.length > 0 ? prune(node.children) : undefined
    }))

  return prune(roots)
}

const loadTenantOptions = async () => {
  tenantLoading.value = true
  try {
    const response = await getTenantTree()
    tenantTreeOptions.value = normalizeTenantNodes(response.data || [])
  } catch (error) {
    console.error('加载租户树失败:', error)
    tenantTreeOptions.value = []
  } finally {
    tenantLoading.value = false
  }
}

const loadUserList = async () => {
  loading.value = true
  try {
    const params: UserQueryParams = {
      username: searchForm.username,
      loginIdentifier: searchForm.loginIdentifier,
      realName: searchForm.realName,
      current: pagination.currentPage,
      size: pagination.pageSize
    }
    const response = await getUserList(params)
    if (response.data) {
      tableData.value = response.data.records || []
      pagination.total = response.data.total || 0
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.warning('加载用户列表失败，显示模拟数据')
    tableData.value = [
      {
        id: 1,
        username: 'admin',
        loginIdentifier: 'admin@example.com',
        realName: '管理员',
        phone: '13800138000',
        email: 'admin@example.com',
        status: 1,
        userType: 1,
        tenantId: 1,
        tenantName: '总部'
      },
      {
        id: 2,
        username: 'operator',
        loginIdentifier: 'operator@example.com',
        realName: '运营人员',
        phone: '13800138001',
        email: 'operator@example.com',
        status: 1,
        userType: 2,
        tenantId: 2,
        tenantName: '运营商 A'
      }
    ] as User[]
    pagination.total = tableData.value.length
  } finally {
    loading.value = false
  }
}

const resetForm = (user?: User) => {
  formData.id = user?.id ?? 0
  formData.username = user?.username ?? ''
  formData.loginIdentifier = user?.loginIdentifier ?? ''
  formData.password = ''
  formData.realName = user?.realName ?? ''
  formData.phone = user?.phone ?? ''
  formData.email = user?.email ?? ''
  formData.status = user?.status ?? 1
  formData.userType = user?.userType ?? 2
  formData.tenantId = user?.tenantId ?? 0
  formRef.value?.clearValidate()
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadUserList()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.loginIdentifier = ''
  searchForm.realName = ''
  pagination.currentPage = 1
  loadUserList()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  resetForm()
  if (!tenantTreeOptions.value.length) {
    void loadTenantOptions()
  }
  dialogVisible.value = true
}

const handleView = (row: User) => {
  router.push(`/users/${row.id}`)
}

const handleEdit = (row: User) => {
  dialogTitle.value = `编辑用户 - ${row.username}`
  resetForm(row)
  if (!tenantTreeOptions.value.length) {
    void loadTenantOptions()
  }
  dialogVisible.value = true
}

const handleResetPassword = async (row: User) => {
  try {
    await ElMessageBox.confirm('确定要重置该用户密码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const newPassword = '123456'
    await resetUserPassword(row.id, newPassword)
    ElMessage.success(`密码重置成功，新密码为: ${newPassword}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

const handleDelete = async (row: User) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteUser(row.id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && pagination.currentPage > 1) {
      pagination.currentPage -= 1
    }
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const buildSubmitPayload = (): UserForm => {
  const payload: UserForm = {
    username: formData.username.trim(),
    loginIdentifier: formData.loginIdentifier?.trim(),
    realName: formData.realName?.trim(),
    phone: formData.phone?.trim(),
    email: formData.email?.trim(),
    status: formData.status,
    userType: formData.userType,
    tenantId: formData.tenantId
  }

  if (!payload.tenantId) {
    throw new Error('请先选择所属租户')
  }

  if (!formData.id) {
    payload.password = formData.password?.trim()
    if (!payload.password) {
      throw new Error('请填写初始密码')
    }
  }

  return payload
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
    const payload = buildSubmitPayload()
    if (formData.id) {
      await updateUser(formData.id, payload)
      ElMessage.success('用户更新成功')
    } else {
      await createUser(payload)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    await loadUserList()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.warning(error.message)
    } else {
      console.error('保存用户失败:', error)
      ElMessage.error('保存失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadUserList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadUserList()
}

onMounted(() => {
  void loadTenantOptions()
  void loadUserList()
})
</script>

<style scoped>
.user-list {
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
