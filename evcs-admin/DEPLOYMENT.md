# EVCS Admin 前端部署指南

## 快速启动

### 开发模式
```bash
cd evcs-admin
npm install
npm run dev
```
访问: http://localhost:3000

### 生产构建
```bash
npm run build
```
构建产物在 `dist/` 目录

### 预览构建结果
```bash
npm run preview
```

---

## Docker 部署

### 方式1: Nginx 静态服务

**创建 Dockerfile**:
```dockerfile
FROM nginx:alpine
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**nginx.conf**:
```nginx
server {
    listen 80;
    server_name localhost;
    
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**构建运行**:
```bash
docker build -t evcs-admin:latest .
docker run -d -p 3000:80 evcs-admin:latest
```

### 方式2: 集成到 docker-compose

**添加到 docker-compose.yml**:
```yaml
  admin-frontend:
    build:
      context: ./evcs-admin
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    networks:
      - evcs-network
    depends_on:
      - gateway
```

---

## API 配置

### 开发环境
`vite.config.ts` 已配置代理:
```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 生产环境
通过 Nginx 反向代理到 Gateway:
```nginx
location /api {
    proxy_pass http://gateway:8080;
}
```

---

## 功能说明

### 已实现页面

#### 核心功能
- ✅ **登录页** - `/login` - JWT认证
- ✅ **仪表盘** - `/dashboard` - 数据概览

#### 租户管理
- ✅ **租户列表** - `/tenants` - 分页、搜索、CRUD
- ✅ **租户详情** - `/tenants/:id` - 详细信息、统计
- ✅ **租户树形** - `/tenants/tree` - 层级关系可视化

#### 用户管理
- ✅ **用户列表** - `/users` - 用户管理
- ✅ **用户详情** - `/users/:id` - 用户信息
- ✅ **角色管理** - `/roles` - 角色权限

#### 充电业务
- ✅ **充电站列表** - `/stations` - 充电站管理
- ✅ **充电站详情** - `/stations/:id`
- ✅ **充电桩列表** - `/chargers` - 充电桩管理
- ✅ **充电桩详情** - `/chargers/:id`

#### 订单管理
- ✅ **订单列表** - `/orders` - 订单查询
- ✅ **订单详情** - `/orders/:id`
- ✅ **订单统计** - `/orders/dashboard` - 数据分析

#### 计费管理
- ✅ **计费方案列表** - `/billing/plans`
- ✅ **计费方案表单** - `/billing/plans/form`

---

## 技术栈

- **Vue 3.5** - Composition API
- **Vite 7.1** - 极速构建
- **TypeScript 5.9** - 类型安全
- **Element Plus 2.11** - UI组件库
- **Vue Router 4.5** - 路由管理
- **Pinia 3.0** - 状态管理
- **Axios 1.12** - HTTP客户端

---

## 开发规范

### 目录结构
```
evcs-admin/
├── src/
│   ├── components/      # 公共组件
│   │   └── Layout.vue   # 布局组件
│   ├── views/           # 页面组件
│   │   ├── Login.vue
│   │   ├── Dashboard.vue
│   │   ├── tenant/      # 租户模块
│   │   ├── user/        # 用户模块
│   │   ├── station/     # 充电站模块
│   │   ├── charger/     # 充电桩模块
│   │   ├── order/       # 订单模块
│   │   └── billing/     # 计费模块
│   ├── router/          # 路由配置
│   ├── utils/           # 工具函数
│   ├── App.vue          # 根组件
│   └── main.ts          # 入口文件
├── public/              # 静态资源
├── dist/                # 构建产物
├── package.json
├── vite.config.ts
└── tsconfig.json
```

### 代码规范
- 使用 TypeScript 严格模式
- 遵循 Vue 3 Composition API
- 组件命名: PascalCase
- 文件命名: PascalCase.vue

---

## 常见问题

### Q: 登录失败？
**A**: 检查后端 Gateway 是否启动 (http://localhost:8080)

### Q: API 调用 404？
**A**: 确认 Vite 代理配置或 Nginx 配置正确

### Q: 构建体积过大？
**A**: 
1. 使用动态 import 代码分割
2. 配置 rollup manualChunks
3. 启用 gzip 压缩

---

## 性能优化

### 已实现
- ✅ 路由懒加载 (dynamic import)
- ✅ Element Plus 按需引入
- ✅ Vite 构建优化
- ✅ TypeScript 编译优化

### 待优化
- [ ] 图片懒加载
- [ ] 虚拟滚动 (长列表)
- [ ] PWA 支持
- [ ] CDN 加速

---

## 部署检查清单

- [ ] 构建成功 (`npm run build`)
- [ ] API 地址配置正确
- [ ] Nginx 配置完成
- [ ] Docker 镜像构建成功
- [ ] 测试登录功能
- [ ] 测试主要业务流程
- [ ] 检查浏览器控制台无错误

---

**版本**: 1.0.0  
**最后更新**: 2025-10-25  
**状态**: ✅ 可用
