<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">
        <el-icon style="font-size: 32px; color: #409eff;"><Lightning /></el-icon>
        <span>EVCS Manager</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-sub-menu index="/tenants">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>租户管理</span>
          </template>
          <el-menu-item index="/tenants">租户列表</el-menu-item>
          <el-menu-item index="/tenants/tree">租户树形</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/users">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/users">用户列表</el-menu-item>
          <el-menu-item index="/roles">角色管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/stations">
          <el-icon><Location /></el-icon>
          <span>充电站管理</span>
        </el-menu-item>
        <el-menu-item index="/chargers">
          <el-icon><Monitor /></el-icon>
          <span>充电桩管理</span>
        </el-menu-item>
        <el-sub-menu index="/orders">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>订单管理</span>
          </template>
          <el-menu-item index="/orders">订单列表</el-menu-item>
          <el-menu-item index="/orders/dashboard">订单统计</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/billing-plans">
          <el-icon><Coin /></el-icon>
          <span>计费方案</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span class="page-title">{{ pageTitle }}</span>
          <div class="user-info">
            <el-dropdown>
              <span class="el-dropdown-link">
                <el-icon><Avatar /></el-icon>
                管理员
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title as string || '')

const handleLogout = () => {
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-aside {
  background-color: #2c3e50;
  color: #fff;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  font-size: 20px;
  font-weight: bold;
  color: #ffffff;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-menu {
  border-right: none;
  background-color: #2c3e50;
}

/* 默认状态 - 未选中，未悬停 */
.sidebar-menu :deep(.el-menu-item) {
  color: #b8c7ce;
  font-weight: 400;
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
}

/* 悬停状态 - 鼠标划过但未选中 */
.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #34495e !important;
  color: #ffffff;
  border-left: 3px solid #409eff;
}

/* 选中状态 - 当前页面 */
.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #ffffff;
  font-weight: 600;
  border-left: 3px solid #66b1ff;
}

/* 选中且悬停状态 */
.sidebar-menu :deep(.el-menu-item.is-active:hover) {
  background-color: #66b1ff !important;
  color: #ffffff;
  border-left: 3px solid #a0cfff;
}

/* 菜单项图标样式 */
.sidebar-menu :deep(.el-menu-item .el-icon) {
  color: inherit;
  transition: all 0.3s ease;
}

/* ========== 子菜单样式 ========== */

/* 子菜单标题 - 默认状态 */
.sidebar-menu :deep(.el-sub-menu__title) {
  color: #b8c7ce;
  font-weight: 400;
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
}

/* 子菜单标题 - 悬停状态 */
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background-color: #34495e !important;
  color: #ffffff;
  border-left: 3px solid #409eff;
}

/* 子菜单标题图标 */
.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  color: inherit;
}

/* ========== 内嵌子菜单（展开式）========== */

/* 子菜单容器背景 */
.sidebar-menu :deep(.el-sub-menu .el-menu) {
  background-color: #344A5F !important;
}

/* 内嵌子菜单项 - 默认状态 */
.sidebar-menu :deep(.el-sub-menu .el-menu .el-menu-item) {
  background-color: #344A5F !important;
  color: #c8d1d9 !important;
  font-weight: 400;
  border-left: 3px solid transparent;
  padding-left: 50px !important;
  min-width: auto !important;
}

/* 内嵌子菜单项 - 悬停状态 */
.sidebar-menu :deep(.el-sub-menu .el-menu .el-menu-item:hover) {
  background-color: #2d3e50 !important;
  color: #ffffff !important;
  border-left: 3px solid #409eff;
}

/* 内嵌子菜单项 - 选中状态 */
.sidebar-menu :deep(.el-sub-menu .el-menu .el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #ffffff !important;
  font-weight: 600;
  border-left: 3px solid #66b1ff;
}

/* 内嵌子菜单项 - 选中且悬停 */
.sidebar-menu :deep(.el-sub-menu .el-menu .el-menu-item.is-active:hover) {
  background-color: #66b1ff !important;
  color: #ffffff !important;
  border-left: 3px solid #a0cfff;
}

/* ========== 弹出式子菜单（备用）========== */

/* 子菜单弹出层背景 */
.sidebar-menu :deep(.el-menu--popup) {
  background-color: #23313f !important;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 子菜单项 - 默认状态 */
.sidebar-menu :deep(.el-menu--popup .el-menu-item) {
  background-color: #23313f !important;
  color: #c8d1d9 !important;
  font-weight: 400;
  border-left: 3px solid transparent;
  padding-left: 40px !important;
}

/* 子菜单项 - 悬停状态 */
.sidebar-menu :deep(.el-menu--popup .el-menu-item:hover) {
  background-color: #2d3e50 !important;
  color: #ffffff !important;
  border-left: 3px solid #3498db;
}

/* 子菜单项 - 选中状态 */
.sidebar-menu :deep(.el-menu--popup .el-menu-item.is-active) {
  background-color: #1a252f !important;
  color: #3498db !important;
  font-weight: 600;
  border-left: 3px solid #3498db;
}

/* 子菜单项 - 选中且悬停 */
.sidebar-menu :deep(.el-menu--popup .el-menu-item.is-active:hover) {
  background-color: #1a252f !important;
  color: #5dade2 !important;
  border-left: 3px solid #5dade2;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
