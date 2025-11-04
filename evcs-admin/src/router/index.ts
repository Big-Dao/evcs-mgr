import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import Layout from '@/components/Layout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'DataAnalysis' }
      },
      {
        path: 'tenants',
        name: 'Tenants',
        component: () => import('@/views/tenant/TenantList.vue'),
        meta: { title: '租户管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'tenants/:id',
        name: 'TenantDetail',
        component: () => import('@/views/tenant/TenantDetail.vue'),
        meta: { title: '租户详情', icon: 'OfficeBuilding', hidden: true }
      },
      {
        path: 'tenants/tree',
        name: 'TenantTree',
        component: () => import('@/views/tenant/TenantTree.vue'),
        meta: { title: '租户树形', icon: 'Tree', hidden: true }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'users/:id',
        name: 'UserDetail',
        component: () => import('@/views/user/UserDetail.vue'),
        meta: { title: '用户详情', icon: 'User', hidden: true }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/user/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Avatar', hidden: true }
      },
      {
        path: 'stations',
        name: 'Stations',
        component: () => import('@/views/station/StationList.vue'),
        meta: { title: '充电站管理', icon: 'Location' }
      },
      {
        path: 'stations/:id',
        name: 'StationDetail',
        component: () => import('@/views/station/StationDetail.vue'),
        meta: { title: '充电站详情', icon: 'Location', hidden: true }
      },
      {
        path: 'chargers',
        name: 'Chargers',
        component: () => import('@/views/charger/ChargerList.vue'),
        meta: { title: '充电桩管理', icon: 'Monitor' }
      },
      {
        path: 'chargers/:id',
        name: 'ChargerDetail',
        component: () => import('@/views/charger/ChargerDetail.vue'),
        meta: { title: '充电桩详情', icon: 'Monitor', hidden: true }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Document' }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetail.vue'),
        meta: { title: '订单详情', icon: 'Document', hidden: true }
      },
      {
        path: 'orders/dashboard',
        name: 'OrderDashboard',
        component: () => import('@/views/order/OrderDashboard.vue'),
        meta: { title: '订单统计', icon: 'DataAnalysis', hidden: true }
      },
      {
        path: 'billing-plans',
        name: 'BillingPlans',
        component: () => import('@/views/billing/BillingPlanList.vue'),
        meta: { title: '计费方案', icon: 'Coin' }
      },
      {
        path: 'billing-plans/new',
        name: 'BillingPlanNew',
        component: () => import('@/views/billing/BillingPlanForm.vue'),
        meta: { title: '新增计费方案', icon: 'Coin', hidden: true }
      },
      {
        path: 'billing-plans/:id/edit',
        name: 'BillingPlanEdit',
        component: () => import('@/views/billing/BillingPlanForm.vue'),
        meta: { title: '编辑计费方案', icon: 'Coin', hidden: true }
      },
      {
        path: 'test',
        name: 'Test',
        component: () => import('@/views/Test.vue'),
        meta: { title: '认证测试', icon: 'Tools' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  console.log('路由跳转:', from.path, '->', to.path)

  // 检查是否需要登录
  if (to.path !== '/login') {
    const token = localStorage.getItem('token')
    console.log('Token检查:', token ? 'exists' : 'not found')

    if (!token) {
      console.log('未找到token，重定向到登录页')
      next('/login')
      return
    }
  }

  next()
})

export default router
