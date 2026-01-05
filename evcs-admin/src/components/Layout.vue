<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">
        <img src="/logo.png" alt="logo" style="width: auto; height: 28px;" />
        <span class="logo-text">铁林慧充</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :default-openeds="['common', 'operation', 'device', 'station', 'finance', 'org']"
        router
        class="sidebar-menu"
        text-color="#606266"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-sub-menu index="common">
          <template #title>
            <div class="common-menu-title">
              <div class="title-content">
                <el-icon><Menu /></el-icon>
                <span>常用</span>
              </div>
              <el-icon class="setting-icon" @click.stop="openCommonConfig"><Setting /></el-icon>
            </div>
          </template>
          <el-menu-item v-for="item in commonMenuItems" :key="item.index" :index="item.index">
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="operation">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>运营</span>
          </template>
          <el-menu-item index="/orders">充电订单</el-menu-item>
          <el-menu-item index="/billing-plans">计费方案</el-menu-item>
          <el-sub-menu index="order-stats">
            <template #title>订单统计</template>
            <el-menu-item index="/orders/dashboard">统计概览</el-menu-item>
            <el-menu-item index="/stations/map-analytics">地区分布</el-menu-item>
          </el-sub-menu>
        </el-sub-menu>

        <el-sub-menu index="device">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>设备</span>
          </template>
          <el-menu-item index="/chargers">充电桩</el-menu-item>
          <el-menu-item index="/connectors">充电枪</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="station">
          <template #title>
            <el-icon><Location /></el-icon>
            <span>场站</span>
          </template>
          <el-menu-item index="/stations">充电站</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="finance">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>财务</span>
          </template>
          <el-menu-item index="/reconciliation">订单结算</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="org">
          <template #title>
            <el-icon><User /></el-icon>
            <span>组织</span>
          </template>
          <el-menu-item index="/tenants">租户管理</el-menu-item>
          <el-menu-item index="/users">账号管理</el-menu-item>
          <el-menu-item index="/roles">角色管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-left">
           <el-breadcrumb separator="/">
            <el-breadcrumb-item>铁林超光(深圳)绿能科技有限公司</el-breadcrumb-item>
            <el-breadcrumb-item>
              <el-dropdown>
                <span class="el-dropdown-link">
                  铁林超光-怡景花园充电站
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>怡景花园充电站</el-dropdown-item>
                    <el-dropdown-item>科技园充电站</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="下载APP" placement="bottom">
            <el-icon class="header-icon"><Download /></el-icon>
          </el-tooltip>
          <el-tooltip content="设置" placement="bottom">
            <el-icon class="header-icon"><Setting /></el-icon>
          </el-tooltip>
          <el-tooltip content="消息" placement="bottom">
            <el-badge is-dot class="item">
              <el-icon class="header-icon"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          <el-dropdown @command="onUserCommand">
            <span class="user-dropdown-link">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">{{ username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- Common Menu Configuration Dialog -->
    <el-dialog
      v-model="configVisible"
      title="常用菜单配置"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="menu-config-container">
        <div v-for="group in allMenus" :key="group.id" class="menu-group">
          <div class="group-title">{{ group.title }}</div>
          <div class="group-items">
            <el-checkbox-group v-model="selectedMenus">
              <el-checkbox 
                v-for="item in group.children" 
                :key="item.index" 
                :label="item.title"
                :value="item.index"
              >
                {{ item.title }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="configVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCommonConfig">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { 
  User, Location, 
  ArrowDown, HomeFilled, Menu, TrendCharts, Tools, Money, Download, Setting, Bell
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const username = computed(() => userStore.username || '管理员')

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
const onUserCommand = (cmd: string) => {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'logout') handleLogout()
}

// Common Menu Configuration
const configVisible = ref(false)
const selectedMenus = ref<string[]>([])
const commonMenuItems = ref<any[]>([])

// Define all available menus for configuration
const allMenus = [
  {
    id: 'operation',
    title: '运营',
    children: [
      { index: '/stations/map-analytics', title: '订单地区分析' },
      { index: '/orders', title: '充电订单' },
      { index: '/billing-plans', title: '计费方案' },
      { index: '/orders/dashboard', title: '订单统计' }
    ]
  },
  {
    id: 'device',
    title: '设备',
    children: [
      { index: '/chargers', title: '充电桩' },
      { index: '/connectors', title: '充电枪' }
    ]
  },
  {
    id: 'station',
    title: '场站',
    children: [
      { index: '/stations', title: '充电站' }
    ]
  },
  {
    id: 'finance',
    title: '财务',
    children: [
      { index: '/orders/dashboard', title: '订单结算' }
    ]
  },
  {
    id: 'org',
    title: '组织',
    children: [
      { index: '/tenants', title: '租户管理' },
      { index: '/users', title: '账号管理' },
      { index: '/roles', title: '角色管理' }
    ]
  }
]

const openCommonConfig = () => {
  // Load current selection from commonMenuItems
  selectedMenus.value = commonMenuItems.value.map(item => item.index)
  configVisible.value = true
}

const saveCommonConfig = () => {
  // Flatten all menus to find titles for selected indices
  const flatMenus = allMenus.flatMap(group => group.children)
  
  const newCommonItems = selectedMenus.value.map(index => {
    const found = flatMenus.find(item => item.index === index)
    return found ? { ...found } : null
  }).filter(item => item !== null)

  commonMenuItems.value = newCommonItems
  
  // Save to localStorage
  localStorage.setItem('user_common_menus', JSON.stringify(newCommonItems))
  
  configVisible.value = false
  ElMessage.success('常用菜单配置已保存')
}

onMounted(() => {
  // Load saved common menus
  const saved = localStorage.getItem('user_common_menus')
  if (saved) {
    try {
      commonMenuItems.value = JSON.parse(saved)
    } catch (e) {
      console.error('Failed to parse common menus', e)
    }
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-aside {
  background-color: #ffffff;
  border-right: 1px solid #e6e6e6;
  transition: width 0.3s;
}

.logo {
  display: flex;
  align-items: center;
  padding-left: 20px;
  height: 60px;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.sidebar-menu {
  border-right: none;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  font-size: 20px;
  color: #606266;
  cursor: pointer;
}

.header-icon:hover {
  color: #409EFF;
}

.user-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 8px;
}

.username {
  font-size: 14px;
  color: #606266;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

/* Menu Item Styles */
:deep(.el-menu-item) {
  border-left: 3px solid transparent;
}

:deep(.el-menu-item.is-active) {
  background-color: #e6f7ff;
  border-left-color: #1890ff;
  color: #1890ff;
}

:deep(.el-sub-menu__title:hover), :deep(.el-menu-item:hover) {
  background-color: #f5f7fa;
}

/* Two-column Submenu Layout */
/* Level 2 Container */
:deep(.el-sub-menu .el-menu) {
  display: flex;
  flex-wrap: wrap;
  padding: 5px;
}

/* Level 2 Items (Leaf) */
:deep(.el-sub-menu .el-menu > .el-menu-item) {
  width: 50%;
  min-width: auto;
  padding: 0 0 0 20px !important;
  height: 36px;
  line-height: 36px;
  margin: 2px 0;
  border-radius: 4px;
  border-left: none;
  font-size: 13px;
}

/* Level 2 Items (Submenu Parent) */
:deep(.el-sub-menu .el-menu > .el-sub-menu) {
  width: 50%;
  min-width: auto;
  margin: 2px 0;
}

/* Level 2 Submenu Title */
:deep(.el-sub-menu .el-menu > .el-sub-menu > .el-sub-menu__title) {
  padding: 0 0 0 20px !important;
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  border-radius: 4px;
}

/* Level 3 Container */
:deep(.el-sub-menu .el-menu > .el-sub-menu .el-menu) {
  display: block;
  width: 100%;
  padding: 0;
}

/* Level 3 Items */
:deep(.el-sub-menu .el-menu > .el-sub-menu .el-menu .el-menu-item) {
  width: 100%;
  padding: 0 0 0 20px !important;
  height: 36px;
  line-height: 36px;
  margin: 0;
  font-size: 13px;
  border-left: none;
}

:deep(.el-sub-menu .el-menu .el-menu-item.is-active) {
  background-color: #e6f7ff;
  color: #1890ff;
  font-weight: bold;
}

/* Common Menu Config Styles */
.common-menu-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 20px; /* Adjust for arrow */
}

.title-content {
  display: flex;
  align-items: center;
  gap: 5px;
}

.setting-icon {
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  display: none; /* Hidden by default */
}

:deep(.el-sub-menu__title:hover) .setting-icon {
  display: block; /* Show on hover */
}

.setting-icon:hover {
  color: #409EFF;
}

.menu-config-container {
  max-height: 400px;
  overflow-y: auto;
}

.menu-group {
  margin-bottom: 15px;
}

.group-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #303133;
  border-left: 3px solid #409EFF;
  padding-left: 8px;
}

.group-items {
  padding-left: 10px;
}

.el-checkbox {
  margin-right: 15px;
  margin-bottom: 5px;
}
</style>
