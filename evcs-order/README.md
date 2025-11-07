# EVCS订单管理服务

> **端口**: 8083 | **状态**: 活跃
>
> **功能**: 订单管理和计费方案

## 📋 服务概述

EVCS订单管理服务负责充电订单的全生命周期管理：
- 充电订单创建和管理
- 计费方案计算
- 订单支付状态跟踪
- 用户账单管理
- 订单统计分析

## 🔧 技术栈
- Spring Boot 3.2.10
- Spring Data JPA
- PostgreSQL
- Redis (缓存)
- RabbitMQ (消息队列)

## 🚀 快速启动

```bash
# 构建和运行
./gradlew :evcs-order:bootRun

# 健康检查
curl http://localhost:8083/actuator/health
```

## 📡 主要API
- `POST /api/v1/orders` - 创建订单
- `GET /api/v1/orders/{id}` - 获取订单详情
- `PUT /api/v1/orders/{id}/payment` - 更新支付状态

## 🔗 相关链接
- [API文档](../../docs/references/API-DOCUMENTATION.md#evcs-order)
- [项目文档](../../docs/README.md)

---

**服务负责人**: 订单团队