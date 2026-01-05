---
applyTo: "**/*Controller.java"
priority: high
---

# API 设计规范 (API Consistency)

> **最后更新**: 2026-01-05 | **维护者**: 架构团队 | **状态**: 已发布

本规范适用于所有 RESTful API 的设计与实现。

## 🚨 关键要求

### 1. URL 风格
**Kebab-case 与 资源导向**
- 所有 URL 路径必须使用小写字母和连字符 (kebab-case)。
- ❌ `/api/getStationList`
- ✅ `/api/v1/charging-stations`
- 必须包含版本号 (v1, v2)。

### 2. HTTP 方法语义
**严格遵守 REST 语义**
- `GET`: 查询资源（幂等，无副作用）
- `POST`: 创建资源（非幂等）
- `PUT`: 全量更新资源（幂等）
- `PATCH`: 部分更新资源（幂等）
- `DELETE`: 删除资源（幂等）

### 3. 响应结构
**统一包装器**
- 所有 API 必须返回统一的 `Result<T>` 结构。
- 禁止直接返回实体类、Map 或 List。

#### 例外：第三方回调 / Webhook
当接口必须满足第三方协议的“固定响应格式”（例如支付回调要求返回纯文本 `success/fail` 或 XML），允许返回 `ResponseEntity<String>` 等原始响应。

约束：
- 仅限“第三方回调/通知”类接口（建议放置在 `*CallbackController` 或 `*WebhookController` 中，并在 OpenAPI `@Tag/@Operation` 标注为回调接口）。
- 必须进行签名/验签与重放保护（如 `nonce/timestamp` 校验）。
- 禁止记录敏感信息（Token、银行卡号、身份证号、密钥等），日志仅记录必要的业务主键与 traceId。

```java
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private String traceId;
}
```

---

## ✅ 代码示例

### 标准 Controller 实现

```java
@RestController
@RequestMapping("/api/v1/charging-stations")
public class StationController {

    @GetMapping
    public Result<Page<StationDTO>> list(StationQuery query) {
        return Result.success(stationService.page(query));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid StationCreateDTO dto) {
        return Result.success(stationService.create(dto));
    }
    
    @GetMapping("/{id}")
    public Result<StationDTO> getById(@PathVariable Long id) {
        return Result.success(stationService.getById(id));
    }
}
```
