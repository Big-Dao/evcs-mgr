# EVCS API设计规范

> **版本**: v1.1 | **最后更�?*: 2025-11-10 | **维护�?*: 技术负责人 | **状�?*: 活跃
>
> 📋 **本文档定义EVCS项目RESTful API的设计标准和规范**

## 🎯 概述

本文档为EVCS充电站管理系统建立统一的API设计标准，确保所有微服务API的一致性、可维护性和易用性�?
## 📋 API设计原则

### 1. RESTful设计
- 使用HTTP动词表示操作：GET（查询）、POST（创建）、PUT（更新）、DELETE（删除）
- 使用名词表示资源，避免动�?- 使用复数形式表示资源集合
- 使用嵌套路径表示资源关系

### 2. 统一响应格式
所有API响应都遵循统一的JSON格式
- 成功响应：包含data字段
- 错误响应：包含error字段
- 分页响应：包含分页信�?
### 3. 版本控制
- 使用URL路径版本控制：`/api/v1/`
- 向后兼容原则
- 废弃API的优雅过�?
## 🏗�?URL设计规范

### 基础URL结构
```
https://evcs.example.com/api/v1/{resource}
```

### 资源命名规范
| 资源类型 | URL示例 | 说明 |
|----------|---------|------|
| 用户管理 | `/api/v1/users` | 用户资源集合 |
| 充电�?| `/api/v1/stations` | 充电站资源集�?|
| 订单 | `/api/v1/orders` | 订单资源集合 |
| 支付 | `/api/v1/payments` | 支付记录集合 |
| 租户 | `/api/v1/tenants` | 租户管理 |

### 嵌套资源设计
```
# 获取特定充电站的充电�?GET /api/v1/stations/{stationId}/charging-poles

# 获取特定用户的订�?GET /api/v1/users/{userId}/orders

# 获取订单的支付记�?GET /api/v1/orders/{orderId}/payments
```

## 📊 HTTP状态码规范

### 成功状态码
| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 OK | 请求成功 | GET、PUT、DELETE成功 |
| 201 Created | 资源创建成功 | POST创建资源 |
| 204 No Content | 请求成功但无返回内容 | DELETE成功 |

### 客户端错误状态码
| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 400 Bad Request | 请求参数错误 | 参数验证失败 |
| 401 Unauthorized | 未认�?| 缺少token或token无效 |
| 403 Forbidden | 无权�?| 认证成功但权限不�?|
| 404 Not Found | 资源不存�?| 请求的资源不存在 |
| 409 Conflict | 资源冲突 | 数据冲突，如重复创建 |
| 422 Unprocessable Entity | 请求格式正确但语义错�?| 业务逻辑验证失败 |

### 服务端错误状态码
| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 500 Internal Server Error | 服务器内部错�?| 未知异常 |
| 502 Bad Gateway | 网关错误 | 下游服务不可�?|
| 503 Service Unavailable | 服务不可�?| 服务暂时不可�?|

## 📦 请求响应格式

### 统一响应结构

#### 成功响应
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    // 实际数据内容
  },
  "timestamp": "2025-11-07T12:00:00Z"
}
```

#### 错误响应
```json
{
  "success": false,
  "code": 400,
  "message": "请求参数错误",
  "error": {
    "type": "VALIDATION_ERROR",
    "details": [
      {
        "field": "orderNo",
        "message": "订单号不能为�?
      }
    ]
  },
  "timestamp": "2025-11-07T12:00:00Z"
}
```

#### 分页响应
```json
{
  "success": true,
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [
      // 数据列表
    ],
    "page": 1,
    "size": 20,
    "total": 100,
    "totalPages": 5
  },
  "timestamp": "2025-11-07T12:00:00Z"
}
```

### 请求格式

#### 查询参数
```http
GET /api/v1/orders?page=1&size=20&status=PENDING&startDate=2025-11-01
```

#### 创建请求
```http
POST /api/v1/orders
Content-Type: application/json

{
  "orderNo": "ORDER-2025-001",
  "stationId": 1,
  "userId": 1,
  "amount": 100.00,
  "chargingDuration": 60
}
```

#### 更新请求
```http
PUT /api/v1/orders/1
Content-Type: application/json

{
  "status": "COMPLETED",
  "endTime": "2025-11-07T12:00:00Z"
}
```

## 🔐 认证授权规范

### JWT Token格式
```http
Authorization: Bearer <jwt_token>
```

#### 多租户上下文头部
```http
X-Tenant-Id: <tenant-id>
X-User-Id: <user-id>
X-Request-Id: <trace-id-optional>
```

### 权限控制注解
```java
@PreAuthorize("hasPermission('order', 'create')")
public ResponseEntity<ApiResponse<OrderDTO>> createOrder(...) {
    // 实现
}

@PreAuthorize("hasRole('ADMIN') or @orderService.isOwner(#orderId, authentication.name)")
public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable Long orderId) {
    // 实现
}
```

### 租户隔离
```java
@DataScope(DataScopeType.TENANT)
public List<OrderDTO> getUserOrders() {
    // 自动添加租户过滤条件
}
```

## 📝 API文档规范

### OpenAPI 3.0规范
```yaml
openapi: 3.0.0
info:
  title: EVCS充电站管理系统API
  version: 1.0.0
  description: 电动汽车充电站管理系统RESTful API

paths:
  /api/v1/orders:
    get:
      summary: 分页查询订单
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: size
          in: query
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 查询成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse'
```

### 注解规范
```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单管理", description = "充电订单的创建、查询、更新等操作")
@Validated
public class OrderController {

    @GetMapping
    @Operation(summary = "分页查询订单", description = "根据条件分页查询订单列表")
    @Parameter(name = "page", description = "页码", example = "1")
    @Parameter(name = "size", description = "每页大小", example = "20")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ResponseEntity<ApiResponse<PageResult<OrderDTO>>> getOrders(
            @ParameterObject PageQueryRequest request) {
        // 实现
    }
}
```

## 🧪 API测试规范

### 单元测试示例
```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("创建订单 - 成功")
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
            .orderNo("TEST-ORDER-001")
            .stationId(1L)
            .amount(new BigDecimal("100.00"))
            .build();

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("TEST-ORDER-001"));
    }
}
```

### 集成测试示例
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndRetrieveOrder() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderNo("INTEGRATION-001");
        request.setAmount(new BigDecimal("100.00"));

        // 创建订单
        ResponseEntity<ApiResponse<OrderDTO>> createResponse = restTemplate.postForEntity(
            "/api/v1/orders", request, new ParameterizedTypeReference<ApiResponse<OrderDTO>>() {});

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 查询订单
        ResponseEntity<ApiResponse<OrderDTO>> getResponse = restTemplate.getForEntity(
            "/api/v1/orders/" + createResponse.getBody().getData().getId(),
            new ParameterizedTypeReference<ApiResponse<OrderDTO>>() {});

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

## 🔍 错误处理规范

### 全局异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(422, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(403, "权限不足"));
    }
}
```

### 错误码规�?| 错误�?| 错误类型 | 说明 |
|--------|----------|------|
| 1001 | USER_NOT_FOUND | 用户不存�?|
| 1002 | PASSWORD_INCORRECT | 密码错误 |
| 2001 | ORDER_NOT_FOUND | 订单不存�?|
| 2002 | ORDER_STATUS_INVALID | 订单状态无�?|
| 3001 | STATION_NOT_FOUND | 充电站不存在 |
| 3002 | STATION_NOT_AVAILABLE | 充电站不可用 |

## 📈 性能优化规范

### 分页查询
```java
@GetMapping
public ResponseEntity<ApiResponse<PageResult<OrderDTO>>> getOrders(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status) {

    // 限制最大页大小
    if (size > 100) {
        size = 100;
    }

    Pageable pageable = PageRequest.of(page - 1, size);
    PageResult<OrderDTO> result = orderService.getOrders(pageable, status);

    return ResponseEntity.ok(ApiResponse.success(result));
}
```

### 缓存策略
```java
@GetMapping("/{id}")
@Cacheable(value = "orders", key = "#id")
public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable Long id) {
    OrderDTO order = orderService.getById(id);
    return ResponseEntity.ok(ApiResponse.success(order));
}

@PutMapping("/{id}")
@CacheEvict(value = "orders", key = "#id")
public ResponseEntity<ApiResponse<OrderDTO>> updateOrder(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrderRequest request) {
    OrderDTO order = orderService.update(id, request);
    return ResponseEntity.ok(ApiResponse.success(order));
}
```

## 🔒 安全规范

### 输入验证
```java
@PostMapping
public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
        @Valid @RequestBody CreateOrderRequest request) {
    // @Valid注解自动验证请求参数
}

public class CreateOrderRequest {
    @NotBlank(message = "订单号不能为�?)
    @Length(max = 50, message = "订单号长度不能超�?0")
    private String orderNo;

    @NotNull(message = "充电站ID不能为空")
    @Positive(message = "充电站ID必须为正�?)
    private Long stationId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    private BigDecimal amount;
}
```

### SQL注入防护
```java
// �?使用参数化查�?@Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId AND o.status = :status")
List<Order> findByTenantAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

// �?避免字符串拼�?// "SELECT * FROM orders WHERE tenant_id = " + tenantId
```

## 📚 相关文档

- [项目编码标准](../overview/PROJECT-CODING-STANDARDS.md)
- [统一测试指南](../testing/TEST-FIX-GUIDE.md)
- [安全配置指南](../troubleshooting/ERROR_PREVENTION_CHECKLIST.md)
- [AI编程助手规范](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANTS-INDEX.md)

---

**遵循本API设计规范可以确保EVCS项目API的一致性、可维护性和安全性�?*
