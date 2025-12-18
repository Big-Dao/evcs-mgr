# API 设计（摘要）

版本：v2.2｜最后更新：2025-12-18｜维护：架构团队｜状态：活跃

用途：提供架构视角的接口设计摘要与示例。通用与详细规范统一收敛于 `docs/development/API-DESIGN-STANDARDS.md`（SSOT）。

## 概述
目标：RESTful 规范、统一请求/响应格式、版本管理、JWT 鉴权、完善的 OpenAPI 文档。

## 认证授权（摘要）
- JWT 认证：所有请求携带 `Authorization: Bearer <token>`。
- 多租户上下文：`X-Tenant-Id`、`X-User-Id`、`X-Request-Id` 必要标识。
- 详细规则：`docs/development/API-DESIGN-STANDARDS.md`。

## 响应格式（摘要）
统一响应结构示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2025-11-02T10:30:00",
  "traceId": "abc123"
}
```

分页响应示例：
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "records": [],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

## 示例
- 路由分层、DTO 与实体转换、异常映射等示例保持在此文件中；规范性条目在 SSOT 中维护。

备注：如需扩展规范或新增规则，请在 `API-DESIGN-STANDARDS.md` 中维护，并在此文件仅保留指向与摘要，避免重复。
