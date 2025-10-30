<template>
  <div class="role-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="roleId" label="角色ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="description" label="角色描述" />
        <el-table-column prop="userCount" label="用户数" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePermission(row)">权限配置</el-button>
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

    <!-- Permission Dialog -->
    <el-dialog v-model="permissionVisible" title="配置角色权限" width="800px">
      <el-tabs v-model="permissionTab">
        <el-tab-pane label="菜单权限" name="menu">
          <el-tree
            ref="menuTreeRef"
            :data="menuPermissions"
            :props="{ label: 'name' }"
            show-checkbox
            node-key="id"
            default-expand-all
            :default-checked-keys="defaultCheckedMenus"
          >
            <template #default="{ data }">
              <span>
                <el-icon style="margin-right: 5px;"><component :is="data.icon" /></el-icon>
                {{ data.name }}
              </span>
            </template>
          </el-tree>
        </el-tab-pane>
        <el-tab-pane label="数据权限" name="data">
          <el-form :model="dataPermissionForm" label-width="120px">
            <el-form-item label="数据范围">
              <el-radio-group v-model="dataPermissionForm.dataScope">
                <el-radio label="ALL">全部数据</el-radio>
                <el-radio label="TENANT">当前租户</el-radio>
                <el-radio label="TENANT_HIERARCHY">当前租户及子租户</el-radio>
                <el-radio label="USER">仅本人数据</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="可见租户" v-if="dataPermissionForm.dataScope === 'CUSTOM'">
              <el-tree
                :data="tenantTree"
                :props="{ label: 'tenantName' }"
                show-checkbox
                node-key="id"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermission">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add/Edit Role Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
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
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const searchForm = reactive({
  name: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 20
})

const tableData = ref([
  {
    roleId: 1,
    roleCode: 'ADMIN',
    roleName: '系统管理员',
    description: '拥有系统所有权限',
    userCount: 5,
    status: 1
  },
  {
    roleId: 2,
    roleCode: 'OPERATOR',
    roleName: '运营人员',
    description: '充电站和订单管理权限',
    userCount: 12,
    status: 1
  },
  {
    roleId: 3,
    roleCode: 'CUSTOMER_SERVICE',
    roleName: '客服人员',
    description: '订单查询和用户服务权限',
    userCount: 8,
    status: 1
  }
])

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const formRef = ref<FormInstance>()

const formData = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  status: 1
})

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const permissionVisible = ref(false)
const permissionTab = ref('menu')
const menuTreeRef = ref()

const menuPermissions = ref([
  {
    id: 1,
    name: '仪表盘',
    icon: 'DataAnalysis'
  },
  {
    id: 2,
    name: '租户管理',
    icon: 'OfficeBuilding',
    children: [
      { id: 21, name: '租户列表', icon: 'List' },
      { id: 22, name: '租户树形', icon: 'Tree' }
    ]
  },
  {
    id: 3,
    name: '用户管理',
    icon: 'User',
    children: [
      { id: 31, name: '用户列表', icon: 'List' },
      { id: 32, name: '角色管理', icon: 'Avatar' }
    ]
  },
  {
    id: 4,
    name: '充电站管理',
    icon: 'Location'
  },
  {
    id: 5,
    name: '充电桩管理',
    icon: 'Monitor'
  },
  {
    id: 6,
    name: '订单管理',
    icon: 'Document'
  },
  {
    id: 7,
    name: '计费方案',
    icon: 'Coin'
  }
])

const defaultCheckedMenus = ref([1, 4, 5, 6])

const dataPermissionForm = reactive({
  dataScope: 'TENANT_HIERARCHY'
})

const tenantTree = ref<any[]>([])

// 加载租户树
const loadTenantTree = async () => {
  try {
    const { getTenantTree } = await import('@/api/tenant')
    const response = await getTenantTree()
    if (response.code === 200 && response.data) {
      const buildTree = (tenants: any[]) => {
        if (!tenants || tenants.length === 0) return []
        const map = new Map()
        tenants.forEach(t => {
          map.set(t.tenantId || t.id, { ...t, id: t.tenantId || t.id, children: [] })
        })
        const tree: any[] = []
        tenants.forEach(t => {
          const node = map.get(t.tenantId || t.id)
          if (t.parentId && map.has(t.parentId)) {
            map.get(t.parentId).children.push(node)
          } else {
            tree.push(node)
          }
        })
        return tree
      }
      const tenants = Array.isArray(response.data) ? response.data : []
      tenantTree.value = buildTree(tenants)
    }
  } catch (error) {
    console.error('加载租户树失败:', error)
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  // TODO: 调用角色列表API
  ElMessage.info('角色查询功能需要后端API支持')
}

const handleReset = () => {
  searchForm.name = ''
}

const handleAdd = () => {
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑角色'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = (_row: any) => {
  ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  })
}

const handlePermission = (row: any) => {
  ElMessage.info('配置角色权限: ' + row.roleName)
  permissionVisible.value = true
  loadTenantTree()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
    }
  })
}

const handleSavePermission = () => {
  ElMessage.success('权限保存成功')
  permissionVisible.value = false
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
}
</script>

<style scoped>
.role-list {
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
