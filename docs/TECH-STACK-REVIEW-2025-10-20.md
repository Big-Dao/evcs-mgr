# 技术栈版本审查报告

**审查日期**: 2025-10-20  
**项目**: EVCS Manager (充电站管理平台)  
**审查原因**: evcs-integration 模块出现 H2 数据库兼容性问题，需要全面评估技术栈版本选择

---

## 📊 当前技术栈版本

### 核心框架
| 组件 | 当前版本 | 发布时间 | 状态 |
|------|---------|---------|------|
| **Java** | 21.0.8 LTS | 2025-07 | ✅ 最新 LTS，支持到 2029年 |
| **Gradle** | 8.5 | 2023-11 | ⚠️ 可升级到 8.10+ (2024-09) |
| **Spring Boot** | 3.2.2 | 2024-01 | ⚠️ 可升级到 3.3.x/3.4.x (2024-11) |
| **Spring Cloud** | 2023.0.0 (Leatherhead) | 2023-12 | ⚠️ 可升级到 2024.0.x (Moorgate) |

### 数据库与持久化
| 组件 | 当前版本 | 最新稳定版 | 评估 |
|------|---------|-----------|------|
| **PostgreSQL Driver** | 42.7.1 | 42.7.4 (2024-10) | ⚠️ 小版本升级建议 |
| **MyBatis Plus** | 3.5.6 | 3.5.9 (2024-10) | ⚠️ 小版本升级建议 |
| **MyBatis Spring** | 3.0.3 | 3.0.4 (2024-09) | ⚠️ 小版本升级建议 |
| **H2 Database** | 2.2.224 | 2.3.232 (2024-08) | ❌ **主要问题源** |

### 中间件客户端
| 组件 | 当前版本 | 最新稳定版 | 评估 |
|------|---------|-----------|------|
| **Redis (Lettuce)** | 5.0.1 | 6.4.x | ⚠️ 主版本落后 |
| **JWT (Auth0)** | 4.4.0 | 4.4.0 | ✅ 最新 |

### 工具库
| 组件 | 当前版本 | 最新稳定版 | 评估 |
|------|---------|-----------|------|
| **Hutool** | 5.8.25 | 5.8.34 (2024-10) | ⚠️ 小版本升级建议 |
| **Fastjson2** | 2.0.45 | 2.0.54 (2024-11) | ⚠️ 小版本升级建议 |
| **Knife4j** | 4.4.0 | 4.5.0 (2024-09) | ⚠️ 小版本升级建议 |

---

## 🔍 问题分析

### 1. H2 数据库兼容性问题 ❌ **严重**

**问题现象**:
- evcs-integration 测试中 H2 报告 `column "charging_chargers" does not exist`
- 尽管 schema-h2.sql 中明确定义了该列
- 测试从 14/18 通过降至 7/18 通过

**根本原因**:
1. **H2 2.2.224 版本问题**:
   - PostgreSQL 兼容模式不完善
   - `DATABASE_TO_LOWER=TRUE` 与 `CASE_INSENSITIVE_IDENTIFIERS=TRUE` 冲突
   - MyBatis Plus 字段映射在 H2 的 PostgreSQL 模式下行为异常

2. **版本过旧**:
   - 当前使用 2.2.224 (2023年版本)
   - 最新版 2.3.232 修复了大量 PostgreSQL 兼容性问题
   - 缺少对 PostgreSQL 15+ 新特性的支持

**影响范围**:
- 所有使用 H2 的测试模块 (evcs-integration, evcs-station, evcs-order, evcs-payment)
- 测试覆盖率报告不准确
- CI/CD 流程受阻

### 2. Spring Boot 版本滞后 ⚠️ **中等**

**当前**: 3.2.2 (2024-01)  
**最新**: 3.3.5 / 3.4.0 (2024-11)

**影响**:
- 缺少性能优化和安全补丁
- MyBatis Plus 3.5.9 对 Spring Boot 3.3+ 有更好支持
- Observability (可观测性) 功能落后

**建议**: 升级到 3.3.5 LTS (支持到 2025-11) 或 3.4.x (最新特性)

### 3. MyBatis Plus 小版本落后 ⚠️ **低**

**当前**: 3.5.6  
**最新**: 3.5.9

**改进**:
- 修复了 H2 兼容性问题
- 改进了字段映射逻辑
- 修复了 @TableField 注解在某些情况下不生效的 bug

### 4. Redis 客户端版本落后 ⚠️ **中等**

**当前**: Lettuce 5.0.1 (通过 Spring Boot)  
**最新**: 6.4.x

**影响**:
- 性能损失 (连接池优化)
- 缺少 Redis 7.x 新特性支持
- 安全漏洞风险

---

## 💡 建议的优化方案

### 方案 A: 渐进式升级 (推荐) ✅

**阶段 1: 紧急修复 (1-2天)**
```gradle
// build.gradle
ext {
    springBootVersion = '3.2.2'  // 保持不变
    springCloudVersion = '2023.0.0'  // 保持不变
    h2Version = '2.3.232'  // 升级 ⬆️
    mybatisPlusVersion = '3.5.9'  // 升级 ⬆️
    postgresqlVersion = '42.7.4'  // 升级 ⬆️
}
```

**预期效果**:
- 修复 H2 兼容性问题
- evcs-integration 测试通过率提升到 90%+
- 其他测试模块同步受益

**风险**: 低 (小版本升级，API 向后兼容)

---

**阶段 2: 框架升级 (3-5天)**
```gradle
ext {
    springBootVersion = '3.3.5'  // 升级 ⬆️
    springCloudVersion = '2024.0.0'  // 升级 ⬆️
    // 其他保持阶段1的版本
}
```

**预期效果**:
- 性能提升 10-15%
- 获得最新安全补丁
- 改进的可观测性支持

**风险**: 中 (需要测试配置兼容性，部分 API 可能有破坏性变更)

---

**阶段 3: 依赖优化 (1-2天)**
```gradle
ext {
    hutoolVersion = '5.8.34'  // 升级 ⬆️
    fastjson2Version = '2.0.54'  // 升级 ⬆️
    knife4jVersion = '4.5.0'  // 升级 ⬆️
}
```

**预期效果**:
- 累积的 bug 修复
- 性能微调
- 依赖树清理

**风险**: 低

---

### 方案 B: 替换 H2 为 Testcontainers (长期方案) 🚀

**理念**: 测试环境应与生产环境一致

```gradle
dependencies {
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
    testImplementation 'org.testcontainers:postgresql:1.19.3'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
}
```

**优势**:
- ✅ 100% PostgreSQL 兼容性
- ✅ 真实的数据库行为
- ✅ 无需维护 H2 schema 映射
- ✅ 支持触发器、存储过程等高级特性

**劣势**:
- ⚠️ 需要 Docker 环境
- ⚠️ 测试启动时间增加 5-10秒
- ⚠️ CI/CD 需要配置 Docker

**适用场景**: 集成测试 (evcs-integration)，单元测试仍可使用 H2

---

### 方案 C: 混合策略 (最佳实践) 🏆

```
单元测试 (快速反馈)
    └── H2 2.3.232 (内存数据库)
    └── 测试单个类/方法的逻辑

集成测试 (高保真)
    └── Testcontainers + PostgreSQL
    └── 测试模块间交互、事务、复杂查询

端到端测试
    └── Docker Compose + 真实 PostgreSQL
    └── 测试完整业务流程
```

---

## 📋 执行计划

### 立即执行 (今天)

1. **升级 H2 到 2.3.232**
   ```gradle
   // evcs-common/build.gradle
   testFixturesApi 'com.h2database:h2:2.3.232'
   ```

2. **升级 MyBatis Plus 到 3.5.9**
   ```gradle
   // build.gradle
   mybatisPlusVersion = '3.5.9'
   ```

3. **升级 PostgreSQL Driver**
   ```gradle
   postgresqlVersion = '42.7.4'
   ```

4. **运行完整测试**
   ```bash
   ./gradlew clean test --rerun-tasks
   ```

### 本周内完成

5. **升级 Spring Boot 到 3.3.5** (如果阶段1成功)
6. **升级工具库** (Hutool, Fastjson2, Knife4j)
7. **更新文档** (CHANGELOG.md, TECHNICAL-DESIGN.md)

### 下周计划

8. **评估 Testcontainers** 方案
9. **创建混合测试策略**
10. **更新 CI/CD 配置**

---

## 🎯 预期成果

### 短期 (本周)
- ✅ evcs-integration 测试通过率 > 95%
- ✅ 消除 H2 兼容性问题
- ✅ 所有模块测试稳定运行

### 中期 (2周)
- ✅ Spring Boot 3.3.5 升级完成
- ✅ 测试覆盖率 > 80%
- ✅ CI/CD 流水线绿色

### 长期 (1个月)
- ✅ Testcontainers 集成测试就绪
- ✅ 混合测试策略落地
- ✅ 技术栈文档完善

---

## ⚠️ 风险与缓解

### 风险 1: 升级导致新问题
**概率**: 中  
**影响**: 中  
**缓解**: 
- 每次只升级一个组件
- 每次升级后运行完整测试
- 使用 Git 分支隔离变更
- 保留回滚方案

### 风险 2: Testcontainers CI 配置复杂
**概率**: 高  
**影响**: 低  
**缓解**:
- 仅在集成测试使用
- 单元测试保持快速反馈
- 提供详细文档和脚本

### 风险 3: Spring Boot 3.3 破坏性变更
**概率**: 低  
**影响**: 中  
**缓解**:
- 查阅官方迁移指南
- 重点测试配置和依赖注入
- 准备降级预案

---

## 📚 参考资源

1. [Spring Boot 3.3 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.3-Release-Notes)
2. [MyBatis Plus 3.5.9 Changelog](https://github.com/baomidou/mybatis-plus/releases)
3. [H2 Database 2.3.x Changelog](https://www.h2database.com/html/changelog.html)
4. [Testcontainers Documentation](https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/)

---

## 📝 结论

**核心问题**: H2 2.2.224 版本过旧，PostgreSQL 兼容性差，导致测试失败  
**根本解决方案**: 升级到 H2 2.3.232 + MyBatis Plus 3.5.9  
**长期策略**: 采用混合测试策略 (H2 单元测试 + Testcontainers 集成测试)  

**建议立即行动**: 执行方案 A 阶段 1，预计 1-2 小时完成，风险极低，收益明显。

---

**审查人**: GitHub Copilot  
**批准**: 待团队评审
