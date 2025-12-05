# EVCS Manager 开发规范
 
> **版本**: v2.1 | **最后更新**: 2025-11-10 | **维护者**: 技术负责人 | **状态**: 活跃
>
> 🧑‍💻 **用途**: 统一编码风格、开发流程与质量要求

## 📋 概述

本文档定义了 EVCS Manager 项目的开发规范，包括编码规范、代码质量标准、开发流程等，确保团队开发的一致性和代码质量。

## 🎯 开发原则

### 1.1 核心原则
- **简单性**: 保持代码简洁明了，避免过度设计
- **一致性**: 遵循统一的编码风格和设计模式
- **可维护性**: 代码易于理解、修改和扩展
- **可测试性**: 代码易于编写测试用例
- **性能优先**: 在满足功能需求的前提下，优先考虑性能

### 1.2 设计原则
- **单一职责原则 (SRP)**: 每个类只负责一个功能
- **开闭原则 (OCP)**: 对扩展开放，对修改关闭
- **里氏替换原则 (LSP)**: 子类可以替换父类
- **接口隔离原则 (ISP)**: 接口应该小而专一
- **依赖倒置原则 (DIP)**: 依赖抽象而不是具体实现

## 📝 编码规范

### 2.1 命名规范

#### 包命名
```java
// 正确示例
package com.evcs.auth.controller;
package com.evcs.station.service.impl;
package com.evcs.common.util;

// 错误示例
package com.evcs.Auth;
package com.evcs.station_service;
package com.evcs.utils;
```

#### 类命名
```java
// 正确示例
public class UserController {
}

public class ChargingOrderService {
}

public class PaymentIntegrationTest {
}

// 错误示例
public class userController {
}

public class chargingorderservice {
}

public class paymenttest {
}
```

#### 方法命名
```java
// 正确示例
public User getUserById(Long userId) {
}

public List<Station> getActiveStations() {
}

public boolean createPayment(PaymentRequest request) {
}

// 错误示例
public User GetUserById(Long userId) {
}

public List<Station> get_active_stations() {
}

public boolean doPayment(PaymentRequest request) {
}
```

#### 变量命名
```java
// 正确示例
private Long tenantId;
private String stationName;
private List<Charger> chargerList;
private LocalDateTime createTime;

// 错误示例
private Long tenant_id;
private String stationname;
private List<Charger> list;
private LocalDateTime time;
```

#### 常量命名
```java
// 正确示例
public static final String DEFAULT_TENANT_CODE = "default";
public static final int MAX_RETRY_COUNT = 3;
public static final long CACHE_EXPIRE_TIME = 3600L;

// 错误示例
public static final String defaultTenantCode = "default";
public static final int max_retry_count = 3;
public static final long cacheExpireTime = 3600l;
```

### 2.2 代码风格

#### 类结构顺序
```java
public class ExampleClass {
    // 1. 静态常量
    private static final String CONSTANT = "value";

    // 2. 静态变量
    private static String staticField;

    // 3. 实例变量 (按访问级别排序: private -> protected -> public)
    @Autowired
    private UserService userService;

    protected String protectedField;

    public String publicField;

    // 4. 构造方法
    public ExampleClass() {
    }

    // 5. 静态方法
    public static ExampleClass create() {
        return new ExampleClass();
    }

    // 6. 实例方法 (按访问级别排序: public -> protected -> private)
    public void publicMethod() {
    }

    protected void protectedMethod() {
    }

    private void privateMethod() {
    }

    // 7. getter/setter方法
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    // 8. 内部类
    private static class InnerClass {
    }
}
```

#### 方法长度
```java
// 正确示例 - 方法简洁，职责单一
public Result<Station> createStation(@Valid CreateStationRequest request) {
    // 参数验证
    validateStationRequest(request);

    // 业务逻辑
    Station station = buildStationFromRequest(request);
    stationService.save(station);

    // 返回结果
    return Result.success(station);
}

private void validateStationRequest(CreateStationRequest request) {
    if (StringUtils.isBlank(request.getStationCode())) {
        throw new BusinessException("充电站编码不能为空");
    }

    if (stationService.existsByCode(request.getStationCode())) {
        throw new BusinessException("充电站编码已存在");
    }
}

// 错误示例 - 方法过长，职责过多
public Result<Station> createStation(@Valid CreateStationRequest request) {
    // 大量验证逻辑
    if (StringUtils.isBlank(request.getStationCode())) {
        throw new BusinessException("充电站编码不能为空");
    }

    if (StringUtils.isBlank(request.getStationName())) {
        throw new BusinessException("充电站名称不能为空");
    }

    // ... 更多验证逻辑

    if (stationService.existsByCode(request.getStationCode())) {
        throw new BusinessException("充电站编码已存在");
    }

    // 构建对象逻辑
    Station station = new Station();
    station.setStationCode(request.getStationCode());
    station.setStationName(request.getStationName());
    // ... 更多设置逻辑

    // 保存逻辑
    stationService.save(station);

    return Result.success(station);
}
```

### 2.3 注释规范

#### 类注释
```java
/**
 * 充电站管理服务
 *
 * <p>提供充电站的增删改查功能，支持多租户数据隔离</p>
 *
 * @author EVCS Team
 * @version 1.0
 * @since 2025-11-01
 */
@Service
public class StationService {
}
```

#### 方法注释
```java
/**
 * 创建充电站
 *
 * @param request 充电站创建请求，不能为null
 * @return 创建成功的充电站信息
 * @throws BusinessException 当充电站编码已存在时抛出
 * @throws IllegalArgumentException 当请求参数无效时抛出
 */
public Station createStation(@Valid CreateStationRequest request) {
    // 实现代码
}
```

#### 复杂逻辑注释
```java
public boolean canAccessStation(Long stationId, Long tenantId) {
    // 检查租户权限：平台方可以访问所有充电站
    if (isSystemAdmin(tenantId)) {
        return true;
    }

    // 检查充电站归属：只能访问自己租户的充电站
    Station station = stationService.getById(stationId);
    if (station == null || !station.getTenantId().equals(tenantId)) {
        return false;
    }

    // 检查租户层级：可以访问子租户的充电站
    return isChildTenant(tenantId, station.getTenantId());
}
```

### 2.4 异常处理规范

#### 自定义异常
```java
// 业务异常
public class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// 租户异常
public class TenantException extends BusinessException {
    public TenantException(String message) {
        super("TENANT_ERROR", message);
    }
}

// 参数异常
public class ParameterException extends BusinessException {
    public ParameterException(String message) {
        super("PARAMETER_ERROR", message);
    }
}
```

#### 异常处理
```java
// 正确示例 - 统一异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(ParameterException.class)
    public Result<Void> handleParameterException(ParameterException e) {
        log.error("参数异常: {}", e.getMessage(), e);
        return Result.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("SYSTEM_ERROR", "系统内部错误，请联系管理员");
    }
}

// 正确示例 - 服务层异常处理
@Service
public class StationService {

    public Station createStation(CreateStationRequest request) {
        try {
            // 业务逻辑
            return stationRepository.save(station);
        } catch (DataIntegrityViolationException e) {
            // 数据完整性异常
            throw new BusinessException("充电站编码已存在");
        } catch (Exception e) {
            // 其他异常
            log.error("创建充电站失败", e);
            throw new BusinessException("创建充电站失败");
        }
    }
}

// 错误示例 - 吞噬异常
@Service
public class StationService {

    public Station createStation(CreateStationRequest request) {
        try {
            return stationRepository.save(station);
        } catch (Exception e) {
            // 错误：吞噬异常，不记录日志
            return null;
        }
    }
}
```

## 🏗️ 架构规范

### 3.1 分层架构

#### Controller层规范
```java
@RestController
@RequestMapping("/station")
@Validated
public class StationController {

    private final StationService stationService;

    @Autowired
    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:view')")
    public Result<Station> getStation(@PathVariable Long id) {
        Station station = stationService.getById(id);
        return Result.success(station);
    }

    @PostMapping
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:add')")
    public Result<Station> createStation(@RequestBody @Valid CreateStationRequest request) {
        Station station = stationService.createStation(request);
        return Result.success(station);
    }
}
```

#### Service层规范
```java
@Service
@Transactional(rollbackFor = Exception.class)
public class StationService {

    private final StationRepository stationRepository;
    private final TenantService tenantService;

    @Autowired
    public StationService(StationRepository stationRepository, TenantService tenantService) {
        this.stationRepository = stationRepository;
        this.tenantService = tenantService;
    }

    @DataScope(value = DataScope.DataScopeType.TENANT)
    public Station createStation(CreateStationRequest request) {
        // 参数验证
        validateCreateRequest(request);

        // 构建对象
        Station station = buildStation(request);

        // 保存数据
        return stationRepository.save(station);
    }

    private void validateCreateRequest(CreateStationRequest request) {
        // 验证逻辑
    }

    private Station buildStation(CreateStationRequest request) {
        // 构建逻辑
    }
}
```

#### Repository层规范
```java
@Repository
public interface StationRepository extends BaseMapper<Station> {

    /**
     * 根据租户ID和充电站编码查询
     */
    default Station getByTenantAndCode(Long tenantId, String stationCode) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
               .eq("station_code", stationCode)
               .eq("deleted", 0);
        return selectOne(wrapper);
    }

    /**
     * 检查充电站编码是否存在
     */
    default boolean existsByTenantAndCode(Long tenantId, String stationCode) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", tenantId)
               .eq("station_code", stationCode)
               .eq("deleted", 0);
        return selectCount(wrapper) > 0;
    }
}
```

### 3.2 数据库规范

#### 表设计规范
```sql
-- 表名使用小写字母和下划线
CREATE TABLE charging_station (
    -- 主键使用表名_id格式
    station_id BIGSERIAL PRIMARY KEY,

    -- 租户字段（所有业务表都必须包含）
    tenant_id BIGINT NOT NULL,

    -- 业务字段使用描述性名称
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(100) NOT NULL,

    -- 状态字段使用status
    status INTEGER NOT NULL DEFAULT 1,

    -- 审计字段（所有表都必须包含）
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    -- 外键约束
    FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id),

    -- 唯一约束
    UNIQUE KEY uk_tenant_code (tenant_id, station_code)
);

-- 索引命名规范
-- 普通索引：idx_表名_字段名
-- 唯一索引：uk_表名_字段名
-- 复合索引：idx_表名_字段名1_字段名2
CREATE INDEX idx_station_tenant_status ON charging_station(tenant_id, status);
CREATE INDEX idx_station_create_time ON charging_station(create_time);
```

#### 实体类规范
```java
@Data
@TableName("charging_station")
public class Station extends BaseEntity {

    /**
     * 充电站ID
     */
    @TableId(value = "station_id", type = IdType.AUTO)
    private Long stationId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 充电站编码
     */
    private String stationCode;

    /**
     * 充电站名称
     */
    private String stationName;

    /**
     * 状态：1-启用，0-停用
     */
    private Integer status;

    /**
     * 地址
     */
    private String address;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;
}
```

### 3.3 API设计规范

#### RESTful API规范
```java
@RestController
@RequestMapping("/api/v1/stations")
public class StationApiController {

    // 获取资源列表
    @GetMapping
    public Result<IPage<Station>> getStations(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
    }

    // 获取单个资源
    @GetMapping("/{id}")
    public Result<Station> getStation(@PathVariable Long id) {
    }

    // 创建资源
    @PostMapping
    public Result<Station> createStation(@RequestBody @Valid CreateStationRequest request) {
    }

    // 更新资源
    @PutMapping("/{id}")
    public Result<Station> updateStation(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStationRequest request) {
    }

    // 删除资源
    @DeleteMapping("/{id}")
    public Result<Void> deleteStation(@PathVariable Long id) {
    }

    // 批量操作
    @PostMapping("/batch")
    public Result<Void> batchOperation(@RequestBody @Valid BatchOperationRequest request) {
    }
}
```

#### 请求响应规范
```java
// 统一响应格式
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String traceId;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(Integer.parseInt(code));
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        result.setTraceId(MDC.get("traceId"));
        return result;
    }
}

// 请求对象
@Data
public class CreateStationRequest {

    @NotBlank(message = "充电站编码不能为空")
    @Length(max = 64, message = "充电站编码长度不能超过64个字符")
    private String stationCode;

    @NotBlank(message = "充电站名称不能为空")
    @Length(max = 100, message = "充电站名称长度不能超过100个字符")
    private String stationName;

    @NotBlank(message = "地址不能为空")
    @Length(max = 200, message = "地址长度不能超过200个字符")
    private String address;

    @DecimalMin(value = "-90.0", message = "纬度范围不正确")
    @DecimalMax(value = "90.0", message = "纬度范围不正确")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "经度范围不正确")
    @DecimalMax(value = "180.0", message = "经度范围不正确")
    private Double longitude;
}
```

## 🧪 测试规范

### 4.1 测试分类

#### 单元测试
```java
@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationService stationService;

    @Test
    @DisplayName("创建充电站 - 成功")
    void createStation_Success() {
        // Given
        CreateStationRequest request = new CreateStationRequest();
        request.setStationCode("ST001");
        request.setStationName("测试充电站");

        Station savedStation = new Station();
        savedStation.setStationId(1L);
        savedStation.setStationCode("ST001");
        savedStation.setStationName("测试充电站");

        when(stationRepository.save(any(Station.class))).thenReturn(savedStation);

        // When
        Station result = stationService.createStation(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStationCode()).isEqualTo("ST001");
        assertThat(result.getStationName()).isEqualTo("测试充电站");

        verify(stationRepository).save(any(Station.class));
    }

    @Test
    @DisplayName("创建充电站 - 编码已存在")
    void createStation_CodeExists() {
        // Given
        CreateStationRequest request = new CreateStationRequest();
        request.setStationCode("ST001");

        when(stationRepository.existsByTenantAndCode(any(), any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> stationService.createStation(request))
            .isInstanceOf(BusinessException.class)
            .hasMessage("充电站编码已存在");
    }
}
```

#### 集成测试
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class StationControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StationService stationService;

    @Test
    @DisplayName("创建充电站API - 成功")
    void createStationApi_Success() {
        // Given
        CreateStationRequest request = new CreateStationRequest();
        request.setStationCode("ST001");
        request.setStationName("测试充电站");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getValidToken());
        HttpEntity<CreateStationRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "/api/v1/stations", entity, Result.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData()).isNotNull();
    }
}
```

### 4.2 测试数据管理

#### 测试数据构建
```java
@Component
public class TestDataBuilder {

    public Station buildStation() {
        return Station.builder()
            .stationId(1L)
            .tenantId(1L)
            .stationCode("ST001")
            .stationName("测试充电站")
            .address("测试地址")
            .latitude(39.9042)
            .longitude(116.4074)
            .status(1)
            .build();
    }

    public User buildUser() {
        return User.builder()
            .userId(1L)
            .tenantId(1L)
            .username("testuser")
            .realName("测试用户")
            .status(1)
            .build();
    }

    public CreateStationRequest buildCreateStationRequest() {
        CreateStationRequest request = new CreateStationRequest();
        request.setStationCode("ST001");
        request.setStationName("测试充电站");
        request.setAddress("测试地址");
        request.setLatitude(39.9042);
        request.setLongitude(116.4074);
        return request;
    }
}
```

#### 测试数据库配置
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

## 🔧 工具和配置

### 5.1 代码质量工具

#### Checkstyle配置
```xml
<!-- checkstyle.xml -->
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>
    <property name="fileExtensions" value="java, properties, xml"/>

    <module name="TreeWalker">
        <module name="OuterTypeFilename"/>
        <module name="IllegalTokenText"/>
        <module name="AvoidEscapedUnicodeCharacters"/>
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
        <module name="AvoidStarImport"/>
        <module name="OneTopLevelClass"/>
        <module name="NoLineWrap"/>
        <module name="EmptyBlock"/>
        <module name="NeedBraces"/>
        <module name="LeftCurly"/>
        <module name="RightCurly"/>
        <module name="WhitespaceAround"/>
        <module name="OneStatementPerLine"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="ArrayTypeStyle"/>
        <module name="MissingSwitchDefault"/>
        <module name="FallThrough"/>
        <module name="UpperEll"/>
        <module name="ModifierOrder"/>
        <module name="EmptyLineSeparator"/>
        <module name="SeparatorWrap"/>
        <module name="PackageName"/>
        <module name="TypeName"/>
        <module name="MemberName"/>
        <module name="ParameterName"/>
        <module name="LocalVariableName"/>
        <module name="ClassTypeParameterName"/>
        <module name="MethodTypeParameterName"/>
        <module name="InterfaceTypeParameterName"/>
        <module name="NoFinalizer"/>
        <module name="GenericWhitespace"/>
        <module name="Indentation"/>
        <module name="AbbreviationAsWordInName"/>
        <module name="OverloadMethodsDeclarationOrder"/>
        <module name="VariableDeclarationUsageDistance"/>
        <module name="CustomImportOrder"/>
        <module name="MethodParamPad"/>
        <module name="ParenPad"/>
        <module name="OperatorWrap"/>
        <module name="AnnotationLocation"/>
        <module name="NonEmptyAtclauseDescription"/>
        <module name="JavadocMethod"/>
        <module name="JavadocType"/>
        <module name="JavadocVariable"/>
        <module name="JavadocStyle"/>
    </module>
</module>
```

#### SpotBugs配置
```xml
<!-- spotbugs-exclude.xml -->
<FindBugsFilter>
    <!-- 排除测试类 -->
    <Match>
        <Class name="~.*Test.*" />
    </Match>

    <!-- 排除生成的代码 -->
    <Match>
        <Class name="~.*\.generated\..*" />
    </Match>

    <!-- 排除特定的低优先级问题 -->
    <Match>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2" />
        <Priority value="3" />
    </Match>
</FindBugsFilter>
```

### 5.2 Git规范

#### Commit消息规范
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**:
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建或工具相关

**示例**:
```
feat(station): 添加充电站创建功能

- 实现充电站基础CRUD操作
- 添加参数验证和异常处理
- 支持多租户数据隔离

Closes #123
```

#### .gitignore配置
```gitignore
# 编译输出
build/
target/
out/
*.class
*.jar
!gradle-wrapper.jar

# IDE文件
.idea/
*.iws
*.iml
*.ipr
.vscode/
.settings/
.project
.classpath

# 日志文件
*.log
logs/

# 临时文件
*.tmp
*.temp
*.swp
*.swo
*~

# 操作系统文件
.DS_Store
Thumbs.db

# 环境配置文件
.env
.env.local
.env.*.local

# 数据库文件
*.db
*.sqlite
*.h2.db
```

### 5.3 IDE配置

#### EditorConfig
```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = space
indent_size = 4

[*.xml]
indent_style = space
indent_size = 4

[*.yml]
indent_style = space
indent_size = 2

[*.properties]
indent_style = space
indent_size = 2

[{*.bat,*.cmd}]
end_of_line = crlf
```

## 📋 代码审查清单

### 6.1 功能性检查
- [ ] 功能是否按需求正确实现
- [ ] 边界条件是否处理
- [ ] 异常情况是否处理
- [ ] 业务逻辑是否正确

### 6.2 代码质量检查
- [ ] 代码是否遵循编码规范
- [ ] 命名是否清晰有意义
- [ ] 方法是否简洁，职责是否单一
- [ ] 是否有重复代码
- [ ] 注释是否清晰准确

### 6.3 性能检查
- [ ] 是否有性能问题
- [ ] 数据库查询是否优化
- [ ] 是否有内存泄漏风险
- [ ] 缓存使用是否合理

### 6.4 安全检查
- [ ] 输入参数是否验证
- [ ] SQL注入风险
- [ ] XSS攻击风险
- [ ] 敏感信息是否保护

### 6.5 测试检查
- [ ] 单元测试是否充分
- [ ] 测试用例是否覆盖边界情况
- [ ] 集成测试是否完整
- [ ] 测试数据是否合理

## 📚 参考资源

### 官方文档
- [阿里巴巴Java开发手册](https://github.com/alibaba/p3c)
- [Spring Boot最佳实践](https://spring.io/guides)
- [PostgreSQL官方文档](https://www.postgresql.org/docs/)
- [Redis官方文档](https://redis.io/documentation)

### 工具文档
- [Checkstyle配置指南](https://checkstyle.sourceforge.io/config.html)
- [SpotBugs用户指南](https://spotbugs.readthedocs.io/)
- [JaCoCo代码覆盖率](https://www.jacoco.org/)

### 最佳实践
- [Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350884)
- [Effective Java](https://www.amazon.com/Effective-Java-3rd-Joshua-Bloch/dp/0134685997)
- [设计模式](https://refactoring.guru/design-patterns)

---

**相关文档**:
- [技术架构设计](../architecture/architecture.md)
- [测试指南](./testing-guide.md)
- [开发环境搭建](./setup.md)
- [贡献指南](./contribution.md)
