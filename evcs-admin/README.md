# EVCS Manager Admin Frontend

充电站管理系统前端管理界面

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 下一代前端构建工具
- **TypeScript** - JavaScript的超集
- **Element Plus** - Vue 3组件库
- **Vue Router** - Vue.js官方路由管理器
- **Pinia** - Vue 3状态管理
- **Axios** - HTTP客户端

## 功能特性

### 已实现功能

- ✅ **登录认证界面** - JWT令牌认证
- ✅ **仪表盘** - 数据统计、图表展示
- ✅ **租户管理** 
  - 租户列表（表格 + 分页 + 搜索）
  - 租户详情页（基础信息 + 统计数据 + 子租户列表）
  - 租户新增/编辑表单（表单验证）
  - 租户树形结构展示（层级关系可视化）
- ✅ **用户管理**
  - 用户列表（表格 + 分页 + 搜索）
  - 用户详情页（基础信息 + 权限信息 + 操作日志）
  - 用户新增/编辑表单
  - 角色管理（角色列表 + 权限配置）
  - 角色权限配置（菜单权限树 + 数据权限）
- ✅ **充电站管理**
  - 充电站列表（列表/卡片视图切换）
  - 充电站详情页（基础信息 + 统计数据 + 充电桩列表 + 地图位置）
  - 充电站新增/编辑表单
- ✅ **充电桩管理**
  - 充电桩列表（状态筛选）
  - 充电桩详情页（基础信息 + 实时数据 + 历史趋势 + 设备状态时间轴）
  - 充电桩配置修改
- ✅ **订单管理**
  - 订单列表（高级搜索 + 导出Excel）
  - 订单详情页（订单信息 + 充电时间轴 + 支付信息 + 计费详情）
  - 订单统计Dashboard（今日统计 + 趋势图表 + 排行榜）
- ✅ **计费方案管理**
  - 计费方案列表
  - 计费方案新增/编辑表单（分时计费 + 固定计费）
  - 分段可视化编辑器（拖拽式时间段配置）
  - 24小时时间轴预览

### 界面设计亮点

1. **响应式布局** - 适配不同屏幕尺寸
2. **多视图模式** - 充电站支持列表/卡片视图切换
3. **可视化预览** - 计费方案24小时时间轴展示
4. **状态指示** - 充电桩状态实时监控（空闲/充电中/故障/离线）
5. **数据统计** - 仪表盘展示核心业务指标

## 开发指南

### 环境要求

- Node.js 16+
- npm 7+

### 安装依赖

\`\`\`bash
npm install
\`\`\`

### 启动开发服务器

\`\`\`bash
npm run dev
\`\`\`

访问 http://localhost:3000

### 构建生产版本

\`\`\`bash
npm run build
\`\`\`

### 预览生产版本

\`\`\`bash
npm run preview
\`\`\`

## 项目结构

\`\`\`
evcs-admin/
├── src/
│   ├── api/              # API接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   │   └── Layout.vue    # 主布局组件
│   ├── router/           # 路由配置
│   │   └── index.ts      # 路由定义
│   ├── store/            # 状态管理
│   ├── types/            # TypeScript类型定义
│   ├── utils/            # 工具函数
│   │   └── request.ts    # HTTP请求封装
│   ├── views/            # 页面组件
│   │   ├── Dashboard.vue # 仪表盘
│   │   ├── Login.vue     # 登录页
│   │   ├── tenant/       # 租户管理
│   │   │   ├── TenantList.vue      # 租户列表
│   │   │   ├── TenantDetail.vue    # 租户详情
│   │   │   └── TenantTree.vue      # 租户树形
│   │   ├── user/         # 用户管理
│   │   │   ├── UserList.vue        # 用户列表
│   │   │   ├── UserDetail.vue      # 用户详情
│   │   │   └── RoleList.vue        # 角色管理
│   │   ├── station/      # 充电站管理
│   │   │   ├── StationList.vue     # 充电站列表
│   │   │   └── StationDetail.vue   # 充电站详情
│   │   ├── charger/      # 充电桩管理
│   │   │   ├── ChargerList.vue     # 充电桩列表
│   │   │   └── ChargerDetail.vue   # 充电桩详情
│   │   ├── order/        # 订单管理
│   │   │   ├── OrderList.vue       # 订单列表
│   │   │   ├── OrderDetail.vue     # 订单详情
│   │   │   └── OrderDashboard.vue  # 订单统计
│   │   └── billing/      # 计费方案管理
│   │       ├── BillingPlanList.vue # 计费方案列表
│   │       └── BillingPlanForm.vue # 计费方案表单
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── index.html            # HTML模板
├── vite.config.ts        # Vite配置
├── tsconfig.json         # TypeScript配置
└── package.json          # 项目配置
\`\`\`

## 后端API集成

后端API代理配置在 `vite.config.ts` 中：

\`\`\`typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
\`\`\`

## 默认登录信息

- 用户名: `admin`
- 密码: `admin123`

## 多租户架构

系统支持多租户隔离，通过JWT token传递租户信息：
- Token存储在localStorage
- 每个API请求自动携带Authorization头
- 后端根据token进行租户数据隔离

## 下一步计划

- [ ] 实现更多图表展示（ECharts集成）
- [ ] 地图功能集成（高德地图/百度地图）
- [ ] WebSocket实时数据推送
- [ ] 完善错误处理和用户提示
- [ ] 添加国际化支持
- [ ] 性能优化和代码分割

## License

ISC
