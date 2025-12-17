# 充电桩列表 API（/charger/list）路径冲突修复记录

> **状态**: 已完成（历史记录）\
> **修复时间**: 2025-11-01\
> **最后更新**: 2025-11-10

本文记录一次典型的 Spring MVC 路径匹配冲突问题：`/charger/list` 被误匹配到 `/{chargerId}`，导致后端返回 500。

## 现象

- 访问前端充电桩列表页 `http://localhost:3000/chargers` 时出现“加载充电桩列表失败/使用模拟数据”等提示
- 后端接口返回 500
- 典型异常：`NumberFormatException: For input string: "list"`

## 根因

后端控制器存在参数化路径（例如 `@GetMapping("/{chargerId}")`）但未对 ID 做格式约束；当缺少对 `/list` 的明确映射时，Spring MVC 会将 `list` 作为路径变量传入，进而触发类型转换失败。

## 修复方案

对控制器做两处调整（示意）：

1. 增加 `/list` 显式映射（保持与 `/page` 兼容）
2. 为 ID 路径变量增加数字正则约束，避免匹配非数字字符串

```java
// before
@GetMapping("/page")
public Result<?> page(...) { ... }

@GetMapping("/{chargerId}")
public Result<?> detail(@PathVariable Long chargerId) { ... }

// after
@GetMapping({"/page", "/list"})
public Result<?> page(...) { ... }

@GetMapping("/{chargerId:\\d+}")
public Result<?> detail(@PathVariable Long chargerId) { ... }
```

## 验证要点

- `GET /api/charger/list` 返回 200（前端依赖）
- `GET /api/charger/page` 返回 200（兼容保留）
- `GET /api/charger/{id}` 仅允许数字 ID，字符串（如 `list`）不再进入该映射

## 相关文档

- [文档总索引](../DOCUMENTATION-INDEX.md)
- [API 文档](../references/API-DOCUMENTATION.md)
- [编码与架构规范](../overview/PROJECT-CODING-STANDARDS.md)
- [Spring MVC 路径匹配规则](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-requestmapping-uri-templates)
