# EVCS Mobile - C端移动应用

基于 UniApp (Vue 3 + TypeScript) 的电动汽车充电站 C端移动应用，支持微信小程序、支付宝小程序及原生 App。

## 功能模块

### 核心功能
- **首页**: 快捷入口、附近充电站、充电中状态展示
- **充电站**: 列表搜索、地图模式、站点详情、充电桩状态
- **扫码充电**: 扫描二维码/手动输入启动充电
- **充电监控**: 实时充电数据、电量、功率、费用
- **订单管理**: 订单列表、订单详情、支付结算
- **个人中心**: 用户信息、钱包充值、车辆管理

### 支持平台
- ✅ 微信小程序
- ✅ 支付宝小程序
- ✅ H5
- ✅ iOS App
- ✅ Android App

## 快速开始

### 环境要求
- Node.js >= 18
- pnpm / npm / yarn

### 安装依赖
```bash
cd evcs-mobile
npm install
```

### 开发模式
```bash
# 微信小程序
npm run dev:mp-weixin

# 支付宝小程序
npm run dev:mp-alipay

# H5
npm run dev:h5

# App
npm run dev:app
```

### 构建发布
```bash
# 微信小程序
npm run build:mp-weixin

# 支付宝小程序
npm run build:mp-alipay

# H5
npm run build:h5

# App
npm run build:app
```

## 项目结构

```
evcs-mobile/
├── src/
│   ├── api/              # API 接口封装
│   │   ├── auth.ts       # 认证相关
│   │   ├── station.ts    # 充电站相关
│   │   ├── order.ts      # 订单相关
│   │   └── payment.ts    # 支付相关
│   ├── components/       # 公共组件
│   ├── hooks/            # 组合式函数
│   ├── pages/            # 页面
│   │   ├── home/         # 首页
│   │   ├── station/      # 充电站
│   │   ├── charging/     # 充电
│   │   ├── order/        # 订单
│   │   ├── user/         # 个人中心
│   │   └── login/        # 登录
│   ├── pages-sub/        # 分包页面
│   │   └── settings/     # 设置
│   ├── static/           # 静态资源
│   ├── stores/           # Pinia 状态管理
│   ├── styles/           # 全局样式
│   ├── types/            # TypeScript 类型定义
│   ├── utils/            # 工具函数
│   ├── App.vue           # 应用入口
│   ├── main.ts           # 主入口
│   ├── manifest.json     # 应用配置
│   └── pages.json        # 页面配置
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 技术栈

- **框架**: UniApp + Vue 3
- **语言**: TypeScript
- **状态管理**: Pinia
- **样式**: SCSS
- **构建工具**: Vite

## 开发规范

### 代码风格
- 使用 Composition API + `<script setup>` 语法
- TypeScript 强类型
- 组件命名使用 PascalCase

### API 调用
- 所有 API 请求封装在 `src/api/` 目录
- 使用统一的请求工具 `src/utils/request.ts`
- 自动处理 Token 和租户 ID

### 状态管理
- 使用 Pinia 管理全局状态
- 用户状态: `useUserStore`
- 充电状态: `useChargingStore`
- 位置状态: `useLocationStore`

## 小程序配置

### 微信小程序
1. 在 `src/manifest.json` 中配置 `mp-weixin.appid`
2. 使用微信开发者工具打开 `dist/dev/mp-weixin` 目录

### 支付宝小程序
1. 在 `src/manifest.json` 中配置 `mp-alipay.appid`
2. 使用支付宝开发者工具打开 `dist/dev/mp-alipay` 目录

## 后端 API

应用依赖以下后端服务（通过 evcs-gateway 统一代理）：

| API | 路径 | 说明 |
|-----|------|------|
| 认证服务 | `/api/v1/auth/*` | 登录、注册、Token |
| 用户服务 | `/api/v1/user/*` | 用户信息 |
| 充电站服务 | `/api/v1/stations/*` | 站点、充电桩 |
| 订单服务 | `/api/v1/orders/*` | 订单管理 |
| 支付服务 | `/api/v1/payment/*` | 支付、钱包 |

## 注意事项

1. **多租户**: 所有请求自动携带 `X-Tenant-Id` 头
2. **位置权限**: 首次使用需授权位置权限
3. **支付**: 微信/支付宝支付需在对应平台配置
4. **地图**: 小程序需申请地图 API Key

## 相关文档

- [项目编码规范](../docs/overview/PROJECT-CODING-STANDARDS.md)
- [前端开发规范](../.github/instructions/frontend.instructions.md)
- [API 设计规范](../.github/instructions/api.instructions.md)
