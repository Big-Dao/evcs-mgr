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
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'stations',
        name: 'Stations',
        component: () => import('@/views/station/StationList.vue'),
        meta: { title: '充电站管理', icon: 'Location' }
      },
      {
        path: 'chargers',
        name: 'Chargers',
        component: () => import('@/views/charger/ChargerList.vue'),
        meta: { title: '充电桩管理', icon: 'Monitor' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Document' }
      },
      {
        path: 'billing-plans',
        name: 'BillingPlans',
        component: () => import('@/views/billing/BillingPlanList.vue'),
        meta: { title: '计费方案', icon: 'Coin' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
