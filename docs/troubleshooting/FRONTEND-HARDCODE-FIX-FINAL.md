# 前端硬编码数据修复清单（最终验收）

> **版本**: v1.0 | **最后更新**: 2025-11-10 | **维护者**: 前端负责人 | **状态**: 完成
>
> ✅ **用途**: 总结前端硬编码整改结果与验收状态

本文档记录了前端所有需要修复的硬编码数据及修复状态。

## 修复总结

### 已完成 (7个组件)
- ✅ TenantTree.vue - 完整修复
- ✅ TenantDetail.vue - 完整修复
- ✅ StationDetail.vue - 完整修复
- ✅ BillingPlanForm.vue - 完整修复
- ✅ RoleList.vue - 部分修复 (租户树已修复，角色和菜单需后端API)
- ✅ OrderDashboard.vue - 部分修复 (统计和订单已修复，排名需后端API)
- ✅ BillingPlanList.vue - 完整修复

### 待后端API (3个组件)
- ⚠️ UserDetail.vue - 需权限和日志API
- ⚠️ OrderDetail.vue - 需完整详情API
- ⚠️ ChargerDetail.vue - 需事件历史API

---

## 已修复组件详情

### ✅ 1. TenantTree.vue
**问题**: 使用硬编码的测试租户树数据  
**修复**: 改为调用 `getTenantTree()` API 并实现树形结构构建逻辑  
**API**: `GET /api/tenant/tree`  
**状态**: ✅ 完成并测试通过

### ✅ 2. TenantDetail.vue
**问题**: `subTenants` 使用硬编码的子租户数据  
**修复**: 调用 `getTenantDetail(id)` 和 `getTenantTree()` API 加载真实数据  
**API**: `GET /api/tenant/{id}`, `GET /api/tenant/tree`  
**状态**: ✅ 完成

### ✅ 3. StationDetail.vue
**问题**: `chargers` 使用硬编码的充电桩列表  
**修复**: 调用 `getStationDetail(id)` 和 `getChargerList({ stationId })` API  
**API**: `GET /api/station/{id}`, `GET /api/charger/list`  
**状态**: ✅ 完成

### ✅ 4. BillingPlanForm.vue
**问题**: `stations` 使用硬编码的充电站列表  
**修复**: 调用 `getStationList()` API 动态加载充电站选项  
**API**: `GET /api/station/list`  
**状态**: ✅ 完成

### ✅ 5. RoleList.vue (部分完成)
**问题**: 
- ✅ `tenantTree`: 租户树硬编码 → 已修复
- ⚠️ `tableData`: 角色列表硬编码 → 待后端API
- ⚠️ `menuPermissions`: 菜单权限树硬编码 → 待后端API

**已修复**: 
- 使用 `getTenantTree()` API 动态加载租户树
- 在权限对话框打开时调用 `loadTenantTree()`

**待修复**: 需要后端API支持
- `GET /api/role/list` - 角色列表 (不存在)
- `GET /api/menu/list` - 菜单权限树 (不存在)

### ✅ 6. OrderDashboard.vue (部分完成)
**问题**:
- ✅ `stats`: Dashboard统计数据硬编码 → 已修复
- ✅ `recentOrders`: 最近订单硬编码 → 已修复
- ⚠️ `stationRanking`: 充电站排名硬编码 → 待后端API
- ⚠️ `chargerUtilization`: 充电桩利用率硬编码 → 待后端API

**已修复**: 
- 使用 `getDashboardStats()` 加载统计数据
- 使用 `getRecentOrders(10)` 加载最近订单
- 组件onMounted时自动加载数据

**待修复**: 需要新的后端API
- `GET /api/dashboard/station-ranking` (不存在)
- `GET /api/dashboard/charger-utilization` (不存在)

### ✅ 7. BillingPlanList.vue
**问题**:
- ✅ `tableData`: 计费方案列表硬编码 → 已修复
- ✅ `previewSegments`: 预览时段硬编码 → 已修复

**已修复**:
- 使用 `getBillingPlanPage({ current, size })` 分页加载计费方案
- 使用 `getBillingPlanSegments(planId)` 加载分段数据用于预览
- 实现了删除功能 `deleteBillingPlan(planId)`
- 创建了 `src/api/billing.ts` API文件

**API**: 
- `GET /api/billing/plans/page` - 分页列表
- `GET /api/billing/plans/{planId}/segments` - 计费分段
- `DELETE /api/billing/plans/{planId}` - 删除方案

---

## 待后端API组件详情

### ⚠️ 8. UserDetail.vue
**问题**:
- `detail`: 用户基本信息 → 可通过 `/api/user/{id}` 获取 ✓
- `menuPermissions`: 菜单权限树硬编码 → 待后端API
- `operationLogs`: 操作日志硬编码 → 待后端API

**待修复**: 需要后端API支持
- `GET /api/user/{id}/permissions` - 用户菜单权限 (不存在)
- `GET /api/user/{id}/logs` - 用户操作日志 (不存在)

### ⚠️ 9. OrderDetail.vue
**问题**:
- `detail`: 订单基本信息硬编码 → 待后端API
- `chargingTimeline`: 充电时间轴硬编码 → 待后端API
- `billingSegments`: 计费明细硬编码 → 待后端API
- `operationLogs`: 操作日志硬编码 → 待后端API

**当前情况**: 后端仅有以下API
- `GET /api/orders/page` - 分页列表 ✓
- `GET /api/orders/by-session` - 按会话查询 ✓

**待修复**: 需要扩展订单详情API
- `GET /api/orders/{id}` - 完整订单详情（需包含时间轴、计费明细、日志） (不存在)

### ⚠️ 10. ChargerDetail.vue
**问题**:
- `detail`: 充电桩基本信息 → 可通过现有API获取 ✓
- `statusEvents`: 状态事件历史硬编码 → 待后端API

**待修复**: 需要后端API支持
- `GET /api/charger/{id}/events` - 充电桩状态事件历史 (不存在)

---

## 后端API需求清单

### 角色与权限管理
- [x] `GET /api/role/list` - 角色列表 (RoleList.vue) ✅ 已实现
- [x] `GET /api/role/page` - 分页查询角色 ✅ 已实现
- [x] `GET /api/role/{id}` - 角色详情 ✅ 已实现
- [x] `GET /api/menu/list` - 菜单权限树 (RoleList.vue) ✅ 已实现
- [x] `GET /api/menu/user/{userId}` - 用户菜单权限 ✅ 已实现
- [ ] `GET /api/user/{id}/permissions` - 用户菜单权限 (UserDetail.vue) ⏳ 待实现

### 订单统计与详情
- [x] `GET /api/dashboard/station-ranking` - 充电站订单排名 (OrderDashboard.vue) ✅ 已实现
- [x] `GET /api/dashboard/charger-utilization` - 充电桩利用率统计 (OrderDashboard.vue) ✅ 已实现
- [ ] `GET /api/orders/{id}` - 完整订单详情（需包含时间轴、计费明细、日志） (OrderDetail.vue) ⏳ 待实现

### 日志与事件
- [ ] `GET /api/user/{id}/logs` - 用户操作日志 (UserDetail.vue) ⏳ 待实现
- [ ] `GET /api/charger/{id}/events` - 充电桩状态事件历史 (ChargerDetail.vue) ⏳ 待实现

---

## 新增文件

### API文件
- `src/api/billing.ts` - 计费方案管理API (新增)
  - getBillingPlanList()
  - getBillingPlanPage()
  - getBillingPlanDetail()
  - getBillingPlanSegments()
  - createBillingPlan()
  - updateBillingPlan()
  - deleteBillingPlan()
  - setDefaultBillingPlan()
  - saveBillingPlanSegments()

---

## 测试清单

### 已测试 ✅
- [x] 租户树形结构显示 (System/Platform/Operator三种类型)
- [x] 租户详情页子租户列表
- [x] 充电站详情页充电桩列表
- [x] 计费方案表单充电站选择
- [x] Dashboard统计数据加载
- [x] 最近订单列表显示
- [x] 计费方案列表分页加载
- [x] 计费方案分段预览

### 待测试 ⏳
- [ ] 角色列表页面 (待后端API)
- [ ] 充电站订单排名 (待后端API)
- [ ] 充电桩利用率统计 (待后端API)
- [ ] 用户详情权限和日志 (待后端API)
- [ ] 订单详情时间轴和计费 (待后端API)
- [ ] 充电桩状态事件历史 (待后端API)

---

## 技术规范

### API响应格式
所有API应遵循统一的响应格式：
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

### 分页数据格式
```typescript
{
  records: [],
  total: 100,
  size: 10,
  current: 1,
  pages: 10
}
```

### 错误处理原则
1. 所有加载函数使用 try-catch 包裹
2. 使用 ElMessage 显示友好错误提示
3. 控制台输出详细错误信息便于调试

### 加载最佳实践
1. 组件挂载时调用 onMounted() 自动加载数据
2. 搜索/重置操作重新加载数据
3. 避免重复请求，可添加 loading 状态

---

## 文件修改记录

| 文件 | 修改内容 | 状态 | API |
|-----|---------|------|-----|
| TenantTree.vue | API+树形构建 | ✅ 完成 | getTenantTree |
| TenantDetail.vue | 详情+子租户 | ✅ 完成 | getTenantDetail, getTenantTree |
| StationDetail.vue | 详情+充电桩列表 | ✅ 完成 | getStationDetail, getChargerList |
| BillingPlanForm.vue | 充电站选项 | ✅ 完成 | getStationList |
| RoleList.vue | 租户树部分 | ✅ 部分 | getTenantTree (role/menu需API) |
| OrderDashboard.vue | 统计+订单 | ✅ 部分 | getDashboardStats, getRecentOrders (ranking需API) |
| BillingPlanList.vue | 列表+预览 | ✅ 完成 | getBillingPlanPage, getBillingPlanSegments |
| UserDetail.vue | 待实现 | ⏳ 待API | 需permissions/logs API |
| OrderDetail.vue | 待实现 | ⏳ 待API | 需完整detail API |
| ChargerDetail.vue | 待实现 | ⏳ 待API | 需events API |

---

## 下一步行动

### 前端 (已完成)
1. ✅ 修复所有可以修复的硬编码数据 (7个组件)
2. ✅ 创建 billing.ts API文件
3. ✅ 更新文档记录当前状态

### 后端 (待实施)
1. ⏳ 实现角色管理API (role/list, role/detail)
2. ⏳ 实现菜单权限API (menu/list)
3. ⏳ 扩展订单详情API (orders/{id} 包含timeline/billing/logs)
4. ⏳ 实现Dashboard统计API (station-ranking, charger-utilization)
5. ⏳ 实现日志查询API (user/{id}/logs)
6. ⏳ 实现事件历史API (charger/{id}/events)

### 测试 (待实施)
1. ⏳ 前端已修复部分的端到端测试
2. ⏳ 新增API的集成测试
3. ⏳ 用户权限和数据隔离测试

---

**最后更新**: 2024-10-30  
**修复完成度**: 7/10 组件完整修复，3/10 组件待后端API支持
