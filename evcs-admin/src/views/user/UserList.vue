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
        <el-form-item label="真实姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, deleteUser, resetUserPassword } from '@/api/user'
import type { User, UserQueryParams } from '@/api/user'

const router = useRouter()
const loading = ref(false)

const searchForm = reactive<UserQueryParams>({
  username: '',
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

// 加载用户列表
const loadUserList = async () => {
  loading.value = true
  try {
    const params: UserQueryParams = {
      username: searchForm.username,
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
    // Fallback to mock data
    tableData.value = [
      {
        id: 1,
        username: 'admin',
        realName: '管理员',
        phone: '13800138000',
        email: 'admin@example.com',
        status: 1,
        userType: 1,
        tenantId: 1
      },
      {
        id: 2,
        username: 'operator',
        realName: '运营人员',
        phone: '13800138001',
        email: 'operator@example.com',
        status: 1,
        userType: 2,
        tenantId: 1
      }
    ] as User[]
    pagination.total = 2
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadUserList()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  loadUserList()
}

const handleAdd = () => {
  ElMessage.info('新增用户功能')
}

const handleView = (row: User) => {
  router.push(`/users/${row.id}`)
}

const handleEdit = (row: User) => {
  ElMessage.info('编辑用户: ' + row.username)
}

const handleResetPassword = async (row: User) => {
  try {
    await ElMessageBox.confirm('确定要重置该用户密码吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const newPassword = '123456' // 默认密码
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
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadUserList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadUserList()
}

// 页面加载时获取数据
onMounted(() => {
  loadUserList()
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
