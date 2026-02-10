# EVCS 项目共享上下文

> **本文档为所有 AI 助手提供共享的项目基础信息**
> **更新日期**: 2026-02-10

## 1. 项目概述

EVCS 是基于 Spring Boot 3.2.12 + Java 21 的微服务架构充电站管理系统，支持多租户隔离。

## 2. 规范入口（SSOT）

必须以以下文档为权威来源，不在本文件重复详细规范：

- 核心编码与架构规范：`docs/overview/PROJECT-CODING-STANDARDS.md`
- AI 助手统一配置：`docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
- 多租户异步上下文：`docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`

## 3. 架构概览

- 架构模式：微服务 + 严格分层（Controller → Service → Domain/Repository）
- 配置中心：Spring Cloud Config
- 注册发现：Eureka
- 异步场景：需显式传播租户、traceId、请求上下文

## 4. 核心功能模块

- 用户管理：多租户用户注册与认证、RBAC
- 充电站管理：站点与设备、状态同步
- 订单管理：计费方案、订单编排
- 支付管理：支付回调、对账与幂等
- 协议处理：OCPP/云快充事件流
- 监控运维：健康检查与指标聚合
- C 端用户模块：`evcs-user`（规划中，详见 RFC）

## 5. 项目特点

- 多租户：请求上下文注入、数据隔离、跨租户禁止拼接
- 微服务：服务间松耦合、可独立部署与扩展
- 安全：JWT 鉴权、接口安全防护、敏感配置走环境变量
- 可观测：统一日志与指标口径，关键路径埋点

## 6. 相关文档

- 开发者指南：`docs/development/DEVELOPER-GUIDE.md`
- 架构设计：`docs/architecture/architecture.md`
- 部署指南：`docs/deployment/DEPLOYMENT-GUIDE.md`
- 监控指南：`docs/operations/MONITORING-GUIDE.md`
- API 文档：`docs/references/API-DOCUMENTATION.md`

## 7. 重要提醒

1. 所有代码与文档更新必须遵循 SSOT 文档。
2. 多租户与异步上下文传播为硬性要求。
3. 禁止硬编码敏感配置；使用环境变量或配置中心。
4. 新功能需补齐必要测试与质量检查。
5. 架构或上下文不确定时先查 SSOT 文档，不臆测。
