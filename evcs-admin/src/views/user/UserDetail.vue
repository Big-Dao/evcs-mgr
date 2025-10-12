<template>
  <div class="user-detail">
    <el-page-header @back="handleBack" content="用户详情">
      <template #extra>
        <el-button type="primary" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button type="warning" @click="handleResetPassword">
          <el-icon><Lock /></el-icon>
          重置密码
        </el-button>
      </template>
    </el-page-header>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>基本信息</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detail.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email }}</el-descriptions-item>
        <el-descriptions-item label="所属租户">{{ detail.tenantName }}</el-descriptions-item>
        <el-descriptions-item label="用户角色">
          <el-tag v-for="role in detail.roles" :key="role" size="small" style="margin-right: 5px;">
            {{ role }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="最后登录时间">{{ detail.lastLoginTime }}</el-descriptions-item>
        <el-descriptions-item label="最后登录IP">{{ detail.lastLoginIp }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>权限信息</span>
      </template>

      <el-tabs>
        <el-tab-pane label="菜单权限">
          <el-tree :data="menuPermissions" :props="{ label: 'name' }" default-expand-all>
            <template #default="{ data }">
              <span>
                <el-icon style="margin-right: 5px;"><component :is="data.icon" /></el-icon>
                {{ data.name }}
              </span>
            </template>
          </el-tree>
        </el-tab-pane>
        <el-tab-pane label="数据权限">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="数据范围">
              <el-tag type="success">当前租户及子租户</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="可见租户">
              <el-tag v-for="tenant in detail.visibleTenants" :key="tenant" size="small" style="margin-right: 5px;">
                {{ tenant }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>操作日志</span>
      </template>

      <el-table :data="operationLogs" style="width: 100%">
        <el-table-column prop="operationTime" label="操作时间" width="180" />
        <el-table-column prop="operationType" label="操作类型" width="120" />
        <el-table-column prop="operationModule" label="操作模块" width="150" />
        <el-table-column prop="operationDesc" label="操作描述" />
        <el-table-column prop="operationIp" label="操作IP" width="150" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const detail = ref({
  userId: 1,
  username: 'admin',
  realName: '管理员',
  phone: '13800138000',
  email: 'admin@example.com',
  tenantName: '总部',
  roles: ['系统管理员', '运营管理'],
  status: 1,
  createTime: '2024-01-01 10:00:00',
  lastLoginTime: '2024-10-12 09:30:00',
  lastLoginIp: '192.168.1.100',
  visibleTenants: ['总部', '华东运营商', '华南运营商']
})

const menuPermissions = ref([
  {
    name: '仪表盘',
    icon: 'DataAnalysis'
  },
  {
    name: '租户管理',
    icon: 'OfficeBuilding',
    children: [
      { name: '租户列表', icon: 'List' },
      { name: '租户树形', icon: 'Tree' }
    ]
  },
  {
    name: '用户管理',
    icon: 'User',
    children: [
      { name: '用户列表', icon: 'List' },
      { name: '角色管理', icon: 'Avatar' }
    ]
  },
  {
    name: '充电站管理',
    icon: 'Location'
  },
  {
    name: '充电桩管理',
    icon: 'Monitor'
  },
  {
    name: '订单管理',
    icon: 'Document'
  }
])

const operationLogs = ref([
  {
    operationTime: '2024-10-12 09:30:00',
    operationType: '登录',
    operationModule: '系统',
    operationDesc: '用户登录系统',
    operationIp: '192.168.1.100'
  },
  {
    operationTime: '2024-10-12 09:25:00',
    operationType: '修改',
    operationModule: '充电站',
    operationDesc: '修改充电站信息: 市中心充电站',
    operationIp: '192.168.1.100'
  },
  {
    operationTime: '2024-10-12 09:20:00',
    operationType: '查询',
    operationModule: '订单',
    operationDesc: '查询订单列表',
    operationIp: '192.168.1.100'
  }
])

const handleBack = () => {
  router.back()
}

const handleEdit = () => {
  ElMessage.info('编辑用户功能')
}

const handleResetPassword = () => {
  ElMessageBox.confirm('确定要重置该用户密码吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('密码重置成功，新密码为: 123456')
  })
}
</script>

<style scoped>
.user-detail {
  height: 100%;
}

.detail-card {
  margin-top: 20px;
}
</style>
