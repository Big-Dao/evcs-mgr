# EVCS Manager 开发者指南

> **最后更新**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 活跃
>
> **注意**: 项目为半成品状态，部分功能未完成

## 概述

本文档为 EVCS Manager 项目的开发者提供全面的开发指南，包括新模块开发、代码规范、多租户开发注意事项和 Git 工作流。

**版本**: v1.0.0

---

## 目录

- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [新模块开发指南](#新模块开发指南)
- [代码规范](#代码规范)
- [多租户开发注意事项](#多租户开发注意事项)
- [Git工作流](#git工作流)
- [测试指南](#测试指南)
- [常见问题](#常见问题)

---

## 快速开始

### 环境准备

1. **安装JDK 21**:
```bash
# 使用SDKMAN
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem

# 验证安装
java -version
```

2. **安装开发工具**:
- IDE: IntelliJ IDEA 2023.3+ 或 Eclipse 2023-12+
- 构建工具: Gradle 8.5+ (使用项目自带的gradlew)
- 版本控制: Git 2.40+
- 数据库客户端: DBeaver 或 DataGrip

3. **克隆项目**:
```bash
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr
```

4. **配置本地环境**:
```bash
# 启动本地基础设施
docker-compose -f docker-compose.local.yml up -d

# 等待服务就绪（约30秒）
docker-compose ps
```

5. **构建项目**:
```bash
# 首次构建
./gradlew build

# 跳过测试的快速构建
./gradlew build -x test
```

6. **运行服务**:
```bash
# 运行网关服务
./gradlew :evcs-gateway:bootRun

# 运行充电站服务
./gradlew :evcs-station:bootRun
```

7. **访问服务**:
- Swagger API文档: http://localhost:8080/doc.html
- Actuator健康检查: http://localhost:8080/actuator/health

### 导入演示数据（可选）

> 初始数据库仅包含基础结构。执行以下脚本可以导入平台租户（1001）的演示计费方案与订单样本，便于前端联调。

- macOS/Linux:
```bash
cat sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```
- Windows PowerShell:
```powershell
Get-Content sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```

### 演示账号与调用示例

- **租户**: `PLATFORM-001` (`tenantId = 1001`)
- **管理员账号**: `admin.east` / `password`
- **获取JWT**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin.east","password":"password","tenantId":1001}'
```
- **调用订单列表**（需携带上下文头）:
```bash
curl "http://localhost:8080/api/order/list?current=1&size=10" \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-Id: 1001" \
  -H "X-User-Id: 1010"
```

---

## 项目结构

```
evcs-mgr/
├── build.gradle                 # 根项目构建配置
├── settings.gradle              # 子模块配置
├── gradle/                      # Gradle包装器
├── docs/                        # 项目文档
├── sql/                         # 数据库脚本
├── scripts/                     # 工具脚本
├── evcs-common/                 # 通用模块
│   ├── src/main/java
│   │   └── com/evcs/common
│   │       ├── tenant/          # 多租户框架
│   │       ├── exception/       # 异常处理
│   │       ├── util/            # 工具类
│   │       ├── config/          # 配置类
│   │       └── ...
│   └── src/testFixtures/        # 测试工具（可被其他模块使用）
│       └── java/com/evcs/common/test
│           ├── base/            # 测试基类
│           ├── helper/          # 测试辅助类
│           └── util/            # 测试工具类
├── evcs-gateway/                # API网关
├── evcs-auth/                   # 认证授权服务
├── evcs-station/                # 充电站管理服务
├── evcs-order/                  # 订单服务
├── evcs-payment/                # 支付服务
├── evcs-protocol/               # 协议对接服务
├── evcs-tenant/                 # 租户管理服务
└── evcs-integration/            # 集成测试模块
```

### 模块说明

| 模块 | 说明 | 端口 |
|------|------|------|
| evcs-common | 公共组件（多租户、异常、工具类等） | - |
| evcs-gateway | API网关（路由、认证、限流） | 8080 |
| evcs-auth | 认证授权（JWT、权限管理） | 8081 |
| evcs-station | 充电站和充电桩管理 | 8082 |
| evcs-order | 订单管理（创建、支付、结算） | 8083 |
| evcs-payment | 支付服务（微信、支付宝、银联） | 8084 |
| evcs-protocol | 协议对接（OCPP、CloudCharge） | 8085 |
| evcs-tenant | 租户管理（租户CRUD、层级关系） | 8086 |

---

## 新模块开发指南

### 1. 创建新模块

#### 1.1 在 `settings.gradle` 中添加模块

```gradle
include 'evcs-new-module'
```

#### 1.2 创建模块目录结构

```bash
mkdir -p evcs-new-module/src/{main,test}/{java,resources}
```

#### 1.3 创建 `build.gradle`

```gradle
plugins {
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
    id 'com.palantir.docker'
}

description = 'New Module Description'

dependencies {
    // 必须依赖
    implementation project(':evcs-common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // 数据库（如需要）
    implementation "org.postgresql:postgresql:${postgresqlVersion}"
    implementation "com.baomidou:mybatis-plus-boot-starter:${mybatisPlusVersion}"
    
    // API文档
    implementation "com.github.xiaoymin:knife4j-openapi3-spring-boot-starter:${knife4jVersion}"
    
    // 注意：仅用于 @PreAuthorize 注解，不启用 Security 过滤器
    implementation 'org.springframework.security:spring-security-core'
    
    // 测试依赖
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation testFixtures(project(':evcs-common'))
}
```

#### 1.4 配置服务（遵循 [Spring Cloud Config 规范](SPRING-CLOUD-CONFIG-CONVENTIONS.md)）

创建服务配置文件 `config-repo/evcs-new-module-local.yml`:

```yaml
spring:
  application:
    name: evcs-new-module
  config:
    import: optional:configserver:http://localhost:8888
  
  # 数据库配置（如需要）
  datasource:
    url: jdbc:postgresql://localhost:5432/evcs
    username: evcs_user
    password: evcs_pass
    driver-class-name: org.postgresql.Driver
  
  # Redis 配置（如需要）
  redis:
    host: localhost
    port: 6379
    database: 0
  
  # RabbitMQ 配置（如需要）
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# 服务端口
server:
  port: 8090

# MyBatis Plus 配置（如需要）
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.evcs.newmodule.entity
  configuration:
    map-underscore-to-camel-case: true
```

**⚠️ 重要配置规则**:
- ❌ **不要**在服务配置中重复定义 JWT 配置（已在 `application-local.yml` 全局配置）
- ❌ **不要**在服务配置中重复定义 Eureka 配置（已在全局配置）
- ✅ **仅配置**本服务特定的数据库、Redis、RabbitMQ、业务参数等
- ✅ 详细规范参考: [SPRING-CLOUD-CONFIG-CONVENTIONS.md](SPRING-CLOUD-CONFIG-CONVENTIONS.md)

### 2. 创建Application类

```java
package com.evcs.newmodule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 新模块启动类
 * 
 * ⚠️ 重要：
 * 1. 必须排除 SecurityAutoConfiguration（除非是 evcs-auth 服务）
 * 2. Gateway 负责 JWT 验证，服务层通过 TenantInterceptor 读取租户上下文
 * 3. 使用 spring-security-core 支持 @PreAuthorize 注解，但不启用过滤器链
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.evcs.common",      // 扫描公共组件
        "com.evcs.newmodule"    // 扫描本模块
    },
    exclude = {SecurityAutoConfiguration.class}  // ⚠️ 必须排除
)
public class NewModuleApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewModuleApplication.class, args);
    }
}
```

### 3. 创建Entity（实体类）

```java
package com.evcs.newmodule.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 示例实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("your_table_name")
public class YourEntity extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（必须，用于多租户隔离）
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 业务字段
     */
    @TableField("field_name")
    private String fieldName;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志（0-未删除 1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
```

### 4. 创建Mapper（数据访问层）

```java
package com.evcs.newmodule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.newmodule.entity.YourEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据访问接口
 */
@Mapper
public interface YourEntityMapper extends BaseMapper<YourEntity> {
    // 继承BaseMapper已有CRUD方法
    // 可添加自定义方法
}
```

### 5. 创建Service（业务逻辑层）

**接口定义**:
```java
package com.evcs.newmodule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.newmodule.entity.YourEntity;

/**
 * 业务逻辑接口
 */
public interface IYourEntityService extends IService<YourEntity> {
    // 自定义业务方法
    boolean customMethod(Long id);
}
```

**实现类**:
```java
package com.evcs.newmodule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.newmodule.entity.YourEntity;
import com.evcs.newmodule.mapper.YourEntityMapper;
import com.evcs.newmodule.service.IYourEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务逻辑实现类
 */
@Service
public class YourEntityServiceImpl 
        extends ServiceImpl<YourEntityMapper, YourEntity> 
        implements IYourEntityService {

    /**
     * 保存实体（自动设置租户ID）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(YourEntity entity) {
        // 设置租户ID（必须）
        entity.setTenantId(TenantContext.getCurrentTenantId());
        entity.setCreateBy(TenantContext.getCurrentUserId());
        return super.save(entity);
    }

    /**
     * 查询列表（自动过滤租户数据）
     */
    @Override
    @DataScope  // 自动添加租户过滤
    public List<YourEntity> list() {
        return super.list();
    }

    /**
     * 自定义业务方法
     */
    @Override
    @DataScope
    public boolean customMethod(Long id) {
        // 业务逻辑
        return true;
    }
}
```

### 6. 创建Controller（控制器层）

```java
package com.evcs.newmodule.controller;

import com.evcs.common.result.Result;
import com.evcs.common.annotation.DataScope;
import com.evcs.newmodule.entity.YourEntity;
import com.evcs.newmodule.service.IYourEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 控制器
 */
@Tag(name = "示例模块", description = "示例模块相关接口")
@RestController
@RequestMapping("/api/your-entities")
@RequiredArgsConstructor
public class YourEntityController {

    private final IYourEntityService yourEntityService;

    /**
     * 创建
     */
    @Operation(summary = "创建实体")
    @PostMapping
    @PreAuthorize("hasPermission('your_entity:create')")
    public Result<YourEntity> create(@Valid @RequestBody YourEntity entity) {
        yourEntityService.save(entity);
        return Result.success(entity);
    }

    /**
     * 查询列表
     */
    @Operation(summary = "查询列表")
    @GetMapping
    @PreAuthorize("hasPermission('your_entity:query')")
    @DataScope  // 租户数据隔离
    public Result<List<YourEntity>> list() {
        List<YourEntity> list = yourEntityService.list();
        return Result.success(list);
    }

    /**
     * 根据ID查询
     */
    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('your_entity:query')")
    public Result<YourEntity> getById(@PathVariable Long id) {
        YourEntity entity = yourEntityService.getById(id);
        return Result.success(entity);
    }

    /**
     * 更新
     */
    @Operation(summary = "更新实体")
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('your_entity:update')")
    public Result<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody YourEntity entity) {
        entity.setId(id);
        yourEntityService.updateById(entity);
        return Result.success();
    }

    /**
     * 删除
     */
    @Operation(summary = "删除实体")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('your_entity:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        yourEntityService.removeById(id);
        return Result.success();
    }
}
```

---

## 代码规范

### Java代码规范

#### 1. 命名规范

- **类名**: 大驼峰（PascalCase），如 `StationService`
- **方法名**: 小驼峰（camelCase），如 `getStationById`
- **常量**: 全大写+下划线，如 `MAX_RETRY_COUNT`
- **变量**: 小驼峰，如 `stationId`
- **包名**: 全小写，如 `com.evcs.station`

#### 2. 注释规范

**类注释**:
```java
/**
 * 充电站服务实现类
 * 
 * 提供充电站的CRUD操作和业务逻辑处理
 * 
 * @author EVCS Team
 * @since 1.0.0
 */
public class StationServiceImpl implements IStationService {
}
```

**方法注释**:
```java
/**
 * 根据ID查询充电站
 * 
 * @param stationId 充电站ID
 * @return 充电站实体，不存在返回null
 */
public Station getById(Long stationId) {
}
```

#### 3. 代码风格

- 使用 **4空格** 缩进，不使用Tab
- 单行长度不超过 **120字符**
- 左花括号不换行，右花括号独占一行
- 方法之间空一行
- import语句按包名排序，分组

#### 4. 异常处理

**使用自定义异常**:
```java
import com.evcs.common.exception.BusinessException;

public void doSomething(Long id) {
    Station station = getById(id);
    if (station == null) {
        throw new BusinessException("充电站不存在", 1002);
    }
}
```

**不要捕获通用异常**:
```java
// ❌ 不推荐
try {
    // ...
} catch (Exception e) {
    // 太宽泛
}

// ✅ 推荐
try {
    // ...
} catch (IOException e) {
    log.error("IO异常", e);
} catch (SQLException e) {
    log.error("数据库异常", e);
}
```

---

## 多租户开发注意事项

### 重要原则

**EVCS Manager使用四层多租户隔离架构，开发时必须遵循以下原则**:

### 1. 实体类必须包含 tenantId

```java
@TableField("tenant_id")
private Long tenantId;
```

### 2. 保存数据时设置租户ID

```java
@Override
public boolean save(Entity entity) {
    // ✅ 必须设置租户ID
    entity.setTenantId(TenantContext.getCurrentTenantId());
    entity.setCreateBy(TenantContext.getCurrentUserId());
    return super.save(entity);
}
```

### 3. 查询方法使用 @DataScope 注解

```java
@Override
@DataScope  // ✅ 自动过滤租户数据
public List<Entity> list() {
    return super.list();
}
```

### 4. 不要在 WHERE 子句中手动添加 tenant_id

```java
// ❌ 错误：手动添加会导致重复过滤
QueryWrapper<Entity> wrapper = new QueryWrapper<>();
wrapper.eq("tenant_id", TenantContext.getCurrentTenantId());

// ✅ 正确：由MyBatis Plus自动添加
QueryWrapper<Entity> wrapper = new QueryWrapper<>();
wrapper.eq("status", 1);  // 只添加业务条件
```

### 5. 系统表需要在 IGNORE_TABLES 中配置

```java
// CustomTenantLineHandler.java
private static final List<String> IGNORE_TABLES = Arrays.asList(
    "sys_tenant",
    "sys_user",
    "sys_role",
    "sys_permission",
    "your_global_table"  // 添加不需要租户隔离的表
);
```

### 6. 测试时设置租户上下文

```java
@Test
void testSomething() {
    // ✅ 测试前设置租户上下文
    TenantContext.setCurrentTenantId(1L);
    TenantContext.setCurrentUserId(1L);
    
    try {
        // 测试代码
    } finally {
        // ✅ 测试后清理
        TenantContext.clear();
    }
}
```

### 7. 跨租户查询需要特殊权限

```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
@DataScope(DataScope.DataScopeType.ALL)  // 查询所有租户数据
public List<Entity> listAll() {
    return super.list();
}
```

---

## Git工作流

### 分支策略

```
main (生产分支)
  └── develop (开发分支)
        ├── feature/xxx (功能分支)
        ├── bugfix/xxx (修复分支)
        └── hotfix/xxx (紧急修复分支)
```

### 分支命名规范

- **功能分支**: `feature/功能描述`，如 `feature/add-payment-module`
- **修复分支**: `bugfix/问题描述`，如 `bugfix/fix-login-error`
- **紧急修复**: `hotfix/问题描述`，如 `hotfix/fix-security-vulnerability`

### 开发流程

#### 1. 创建功能分支

```bash
# 从develop创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

#### 2. 开发和提交

```bash
# 添加文件
git add .

# 提交（遵循提交规范）
git commit -m "feat: 添加支付模块"
```

#### 3. 推送分支

```bash
git push origin feature/your-feature-name
```

#### 4. 创建Pull Request

在GitHub上创建PR，填写以下信息:
- 标题: 清晰描述功能
- 描述: 详细说明改动内容
- 关联Issue: 如果有相关Issue

#### 5. 代码审查

至少一位团队成员审查通过后才能合并。

#### 6. 合并到develop

审查通过后，由维护者合并到develop分支。

### 提交信息规范

使用约定式提交（Conventional Commits）:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**:
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat(station): 添加充电站批量导入功能

- 支持Excel文件导入
- 实现数据校验
- 添加导入进度显示

Closes #123
```

---

## 测试指南

### 单元测试

**使用测试基类**:
```java
import com.evcs.common.test.base.BaseServiceTest;

@SpringBootTest(classes = YourApplication.class)
class YourServiceTest extends BaseServiceTest {
    
    @Resource
    private IYourService yourService;
    
    @Test
    @DisplayName("测试创建功能")
    void testCreate() {
        // Arrange
        YourEntity entity = new YourEntity();
        entity.setName("test");
        
        // Act
        boolean result = yourService.save(entity);
        
        // Assert
        assertTrue(result);
        assertNotNull(entity.getId());
        assertEquals(DEFAULT_TENANT_ID, entity.getTenantId());
    }
}
```

### 集成测试

```java
import com.evcs.common.test.base.BaseIntegrationTest;

@SpringBootTest(classes = YourApplication.class)
class YourIntegrationTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("端到端测试")
    void testEndToEnd() {
        // 测试完整业务流程
    }
}
```

### 测试覆盖率

运行测试并生成覆盖率报告:

```bash
./gradlew test jacocoTestReport
```

查看报告:
```bash
open evcs-your-module/build/reports/jacoco/test/html/index.html
```

**目标**: 单元测试覆盖率 ≥ 80%

---

## 常见问题

### Q1: 如何添加新的依赖？

在模块的 `build.gradle` 中添加依赖，然后刷新Gradle项目。

### Q2: 租户上下文什么时候设置？

租户上下文由 `JwtAuthFilter` 在请求进入时自动设置，开发者无需手动设置（测试除外）。

### Q3: 如何调试多租户隔离？

打开MyBatis Plus的SQL日志：
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### Q4: 如何处理循环依赖？

避免模块间的循环依赖，使用事件或API调用替代直接依赖。

### Q5: 数据库迁移脚本放在哪里？

放在 `sql/` 目录下，命名格式: `V版本号__描述.sql`

---

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2025-10-12 | 初始版本 |

---

## 支持与联系

**开发问题**: dev@evcs-mgr.com  
**技术讨论**: Slack #evcs-dev 频道  
**文档反馈**: docs@evcs-mgr.com
