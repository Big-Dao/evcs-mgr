# 前端硬编码数据修复清单

本文档记录了前端所有需要修复的硬编码数据，以及修复方案。

## 已修复

### ✅ 1. TenantTree.vue
**问题**: 使用硬编码的测试租户树数据  
**修复**: 已改为调用 `getTenantTree()` API 并实现树形结构构建逻辑  
**状态**: 已完成并测试通过

### ✅ 2. TenantDetail.vue
**问题**: `subTenants` 使用硬编码的子租户数据  
**修复**: 调用 `getTenantTree()` API 并过滤出当前租户的子租户  
**API**: `/api/tenant/{id}`, `/api/tenant/tree`  
**状态**: 已完成

### ✅ 3. StationDetail.vue
**问题**: `chargers` 使用硬编码的充电桩列表  
**修复**: 调用 `getChargerList({ stationId })` API 加载真实数据  
**API**: `/api/station/{id}`, `/api/charger/list`  
**状态**: 已完成

### ✅ 4. BillingPlanForm.vue
**问题**: `stations` 使用硬编码的充电站列表  
**修复**: 调用 `getStationList()` API 加载充电站选项  
**API**: `/api/station/list`  
**状态**: 已完成

### ✅ 5. RoleList.vue (部分完成)
**问题**: 
- `tenantTree`: 租户树硬编码 ✅ 已修复
- `tableData`: 角色列表硬编码 ⚠️ 待后端API
- `menuPermissions`: 菜单权限树硬编码 ⚠️ 待后端API

**已修复**: 使用 `getTenantTree()` API 动态加载租户树，在权限对话框打开时调用  
**待修复**: 角色列表和菜单权限需要后端API支持
- `GET /api/role/list` - 角色列表 (不存在)
- `GET /api/menu/list` - 菜单权限树 (不存在)

### ✅ 6. OrderDashboard.vue (部分完成)
**问题**:
- `stats`: Dashboard统计数据硬编码 ✅ 已修复
- `recentOrders`: 最近订单硬编码 ✅ 已修复
- `stationRanking`: 充电站排名硬编码 ⚠️ 待后端API
- `chargerUtilization`: 充电桩利用率硬编码 ⚠️ 待后端API

**已修复**: 
- 使用 `/api/dashboard/statistics` 加载统计数据
- 使用 `/api/dashboard/recent-orders` 加载最近订单

**待修复**: 充电站排名和充电桩利用率需要新的后端API
- `GET /api/dashboard/station-ranking` (不存在)
- `GET /api/dashboard/charger-utilization` (不存在)

### ✅ 7. BillingPlanList.vue
**问题**:
- `tableData`: 计费方案列表硬编码 ✅ 已修复
- `previewSegments`: 预览时段硬编码 ✅ 已修复

**已修复**:
- 使用 `/api/billing/plans/page` 分页加载计费方案列表
- 使用 `/api/billing/plans/{planId}/segments` 加载计费方案分段用于预览
- 创建了 `src/api/billing.ts` API文件
- 实现了完整的增删改查功能

## 待修复

### 🟡 8. UserDetail.vue (优先级: 中)
```typescript
import { getBillingPlanList } from '@/api/billing'

const loadBillingPlans = async () => {
  const response = await getBillingPlanList(searchForm)
  if (response.code === 200) {
    tableData.value = response.data.records || []
    pagination.total = response.data.total || 0
  }
}

const handlePreview = async (row: any) => {
  // 从row中获取真实的时段数据
  previewSegments.value = row.segments || []
  previewVisible.value = true
}
```

**所需后端API**:
- `GET /api/billing/list` - 计费方案列表

### 🟡 8. UserDetail.vue (优先级: 中)
**问题**:
- `menuPermissions`: 用户菜单权限硬编码
- `operationLogs`: 操作日志硬编码

**修复方案**:
```typescript
import { getUserPermissions, getUserLogs } from '@/api/user'

const loadUserPermissions = async (userId: number) => {
  const response = await getUserPermissions(userId)
  if (response.code === 200) {
    menuPermissions.value = response.data || []
  }
}

const loadOperationLogs = async (userId: number) => {
  const response = await getUserLogs(userId, { current: 1, size: 10 })
  if (response.code === 200) {
    operationLogs.value = response.data.records || []
  }
}
```

**所需后端API**:
- `GET /api/user/{id}/permissions` - 用户权限
- `GET /api/user/{id}/logs` - 操作日志

### 🟡 9. OrderDetail.vue (优先级: 中)
**问题**:
- `chargingTimeline`: 充电时间轴硬编码
- `billingSegments`: 计费明细硬编码
- `operationLogs`: 操作日志硬编码

**修复方案**:
```typescript
import { getOrderDetail, getOrderTimeline, getOrderBilling } from '@/api/order'

const loadOrderDetail = async (orderId: number) => {
  const response = await getOrderDetail(orderId)
  if (response.code === 200 && response.data) {
    // 订单详情
    detail.value = response.data
    
    // 充电时间轴
    chargingTimeline.value = response.data.timeline || []
    
    // 计费明细
    billingSegments.value = response.data.billing || []
    
    // 操作日志
    operationLogs.value = response.data.logs || []
  }
}
```

**所需后端API**:
- `GET /api/order/{id}` - 订单详情（包含时间轴、计费明细、日志）

### 🟢 10. ChargerDetail.vue (优先级: 低)
**问题**:
- `statusEvents`: 状态事件历史硬编码

**修复方案**:
```typescript
import { getChargerStatusEvents } from '@/api/charger'

const loadStatusEvents = async (chargerId: number) => {
  const response = await getChargerStatusEvents(chargerId, { current: 1, size: 20 })
  if (response.code === 200) {
    statusEvents.value = response.data.records || []
  }
}
```

**所需后端API**:
- `GET /api/charger/{id}/events` - 充电桩状态事件历史

## 后续步骤

### 立即执行（本次提交）
1. ✅ TenantTree.vue
2. ✅ TenantDetail.vue  
3. ✅ StationDetail.vue
4. ✅ BillingPlanForm.vue

### 需要后端支持的功能
以下功能需要后端先实现相应的API：

#### 角色与权限管理
- [ ] `GET /api/role/list` - 角色列表
- [ ] `GET /api/role/{id}` - 角色详情
- [ ] `GET /api/menu/list` - 菜单权限树
- [ ] `GET /api/user/{id}/permissions` - 用户权限

#### 订单统计与详情
- [ ] `GET /api/order/dashboard` - 订单仪表板
- [ ] `GET /api/order/{id}` - 完整订单详情（含时间轴、计费、日志）

#### 计费管理
- [ ] `GET /api/billing/list` - 计费方案列表
- [ ] `GET /api/billing/{id}` - 计费方案详情

#### 日志与事件
- [ ] `GET /api/user/{id}/logs` - 用户操作日志
- [ ] `GET /api/charger/{id}/events` - 充电桩状态事件

## 测试清单

### 已测试
- [x] 租户树形结构显示
- [x] 租户详情页子租户列表
- [x] 充电站详情页充电桩列表
- [x] 计费方案表单充电站选择

### 待测试
- [ ] 角色列表页面
- [ ] 订单仪表板统计
- [ ] 计费方案列表
- [ ] 用户详情权限和日志
- [ ] 订单详情时间轴和计费
- [ ] 充电桩状态事件历史

## 注意事项

1. **API响应格式**: 所有API应遵循统一的响应格式：
```typescript
{
  code: 200,
  message: "成功",
  data: any,
  success: true,
  timestamp: "2025-10-30T10:00:00",
  traceId: "xxx"
}
```

2. **分页数据格式**:
```typescript
{
  records: [],
  total: 100,
  size: 10,
  current: 1,
  pages: 10
}
```

3. **错误处理**: 所有加载函数都应包含try-catch并显示友好错误提示

4. **加载状态**: 需要添加loading状态避免重复请求

5. **类型安全**: 优先使用TypeScript类型定义，避免使用any

## 文件修改记录

| 文件 | 修改内容 | 状态 |
|-----|---------|------|
| TenantTree.vue | 调用API+树形构建 | ✅ 完成 |
| TenantDetail.vue | 加载详情+子租户 | ✅ 完成 |
| StationDetail.vue | 加载详情+充电桩列表 | ✅ 完成 |
| BillingPlanForm.vue | 加载充电站选项 | ✅ 完成 |
| RoleList.vue | 需要角色+菜单API | ⏳ 待后端 |
| OrderDashboard.vue | 需要仪表板API | ⏳ 待后端 |
| BillingPlanList.vue | 需要计费方案API | ⏳ 待后端 |
| UserDetail.vue | 需要权限+日志API | ⏳ 待后端 |
| OrderDetail.vue | 需要订单详情API | ⏳ 待后端 |
| ChargerDetail.vue | 需要事件历史API | ⏳ 待后端 |
