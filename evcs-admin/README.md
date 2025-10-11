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

- ✅ 登录认证界面
- ✅ 仪表盘（数据统计、图表）
- ✅ 租户管理（列表、新增、编辑、删除）
- ✅ 用户管理（列表、角色分配）
- ✅ 充电站管理（列表/卡片视图切换、地图展示）
- ✅ 充电桩管理（状态监控、实时数据）
- ✅ 订单管理（高级搜索、导出Excel）
- ✅ 计费方案管理（分时计费、24小时时间轴预览）

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
│   │   ├── user/         # 用户管理
│   │   ├── station/      # 充电站管理
│   │   ├── charger/      # 充电桩管理
│   │   ├── order/        # 订单管理
│   │   └── billing/      # 计费方案管理
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
