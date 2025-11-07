# OpenAI CodeX/EVCS项目规范提示词模板

## 🎯 适用于ChatGPT/Claude/CodeX的编程提示词

当使用OpenAI CodeX或其他AI编程助手时，请在代码生成请求前包含以下上下文：

```
你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：

🏗️ 项目架构：
- Spring Boot 3.2.10 + Java 21微服务架构
- 多租户数据隔离（tenant_id）
- DDD分层：Controller → Service → Repository → Entity
- 严禁跨服务直接访问数据库

📝 编码标准：
- Controller: @RestController, @Validated, @Slf4j, @PreAuthorize
- Service: @Service, @Transactional, @Slf4j, @Cacheable/@CacheEvict, @DataScope
- Entity: 继承BaseEntity, 包含tenant_id, 使用@PrePersist/@PreUpdate
- 使用@Valid @RequestBody进行输入验证
- 使用log.info()而不是System.out.println()

🚫 禁止模式：
- 跨服务数据库访问（如OrderService中注入UserRepository）
- 硬编码敏感信息（密码、密钥、appId等）
- 空catch块或忽略异常处理
- 直接返回实体，必须使用DTO

✅ 必须包含：
- 异常处理和日志记录
- 事务管理（@Transactional）
- 缓存管理（Spring Cache注解）
- 租户上下文管理（TenantContext）
- 单元测试（覆盖率>=80%）

现在请为[具体需求]生成符合上述规范的代码。
```

## 📋 具体使用示例

### 示例1：生成Service类代码
```
你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：

[上述规范内容...]

现在请为充电站管理模块生成StationService类，包含：
1. 创建充电站方法
2. 分页查询充电站方法
3. 更新充电站状态方法
4. 删除充电站方法

每个方法都要包含完整的异常处理、日志记录、缓存管理和租户隔离。
```

### 示例2：生成Controller类代码
```
你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：

[上述规范内容...]

现在请为订单管理模块生成OrderController类，包含：
1. 创建订单 POST /api/v1/orders
2. 查询订单 GET /api/v1/orders/{id}
3. 分页查询订单 GET /api/v1/orders
4. 取消订单 PUT /api/v1/orders/{id}/cancel

每个接口都要包含权限检查、输入验证和统一响应格式。
```

### 示例3：生成Entity类代码
```
你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：

[上述规范内容...]

现在请为充电桩设备生成ChargingStationEntity类，包含：
1. 充电桩基本信息（编号、名称、位置、功率等）
2. 状态管理（可用、占用、故障、维护等）
3. 租户隔离和审计字段
4. 适当的索引和约束

请确保包含JPA注解、验证注解和生命周期回调方法。
```

## 🔍 代码质量检查清单

生成代码后，请验证以下项目：

### 架构合规性
- [ ] 是否遵循DDD分层架构
- [ ] 是否包含跨服务数据库访问
- [ ] 是否正确实现租户隔离
- [ ] 是否包含适当的注解（@Service, @Transactional等）

### 安全性
- [ ] 是否包含权限检查（@PreAuthorize）
- [ ] 是否进行输入验证（@Valid）
- [ ] 是否硬编码敏感信息
- [ ] 是否正确处理异常

### 性能优化
- [ ] 是否使用缓存（@Cacheable/@CacheEvict）
- [ ] 是否避免N+1查询问题
- [ ] 是否合理使用异步处理
- [ ] 是否正确管理资源

### 测试覆盖
- [ ] 是否包含单元测试
- [ ] 测试覆盖率是否达到要求
- [ ] 是否测试边界条件
- [ ] 是否测试异常情况

## 🚀 快速集成方法

### 1. IDE插件集成
如果你使用支持自定义指令的IDE（如VS Code），可以将上述提示词保存为代码片段。

### 2. API调用集成
```python
# 使用OpenAI API的示例
import openai

def generate_evcs_code(requirement):
    context = """
    你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：
    [完整规范内容...]
    """

    prompt = f"{context}\n\n现在请为以下需求生成代码：\n{requirement}"

    response = openai.Completion.create(
        engine="code-davinci-002",
        prompt=prompt,
        temperature=0.1,
        max_tokens=2000
    )

    return response.choices[0].text
```

### 3. 持续学习配置
CodeX会从项目的现有代码中学习模式，确保：
- 项目中有足够的规范代码示例
- 定期review和修正AI生成的代码
- 保持代码风格的一致性

通过以上方法，OpenAI CodeX可以生成符合EVCS项目规范的高质量代码。