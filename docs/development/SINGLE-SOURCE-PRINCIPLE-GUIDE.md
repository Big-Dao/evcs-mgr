# 单一头源原则文档指南

> **最后更新**: 2025-11-07 21:55 | **维护者**: 架构团队

## 🎯 原则概述

### 单一头源原则 (Single Source of Truth)
**所有信息只在一个地方维护，其他地方只引用不重复。**

### 📋 适用范围
- 微服务README文件
- 配置文档
- API文档
- 开发规范

## ✅ 正确做法

### 微服务README结构
```markdown
# 服务名称

> **服务信息**: 完整信息请参考项目文档

## 🔗 服务文档

| 信息类型 | 文档来源 |
|---------|---------|
| **服务功能与职责** | [架构文档 - 服务章节](../../docs/architecture/architecture.md#{service-name}) |
| **技术栈与依赖** | [架构文档 - 技术栈](../../docs/architecture/architecture.md#技术栈) |
| **API接口规范** | [API文档](../../docs/references/API-DOCUMENTATION.md#{service-name}) |
| **开发规范与指南** | [开发指南](../../docs/development/DEVELOPER-GUIDE.md#{service-name}) |
| **部署配置说明** | [部署指南](../../docs/deployment/DEPLOYMENT-GUIDE.md#{service-name}) |
| **测试策略与用例** | [测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md#{service-name}) |
| **监控与运维** | [监控指南](../../docs/operations/MONITORING-GUIDE.md#{service-name}) |
| **端口与配置** | [项目结构](../../docs/operations/PROJECT-STRUCTURE.md#{service-name}) |
| **团队与负责人** | [团队配置](../../docs/operations/TEAM-CONFIGURATION.md#{service-name}) |

## 🎯 使用原则

1. **本README不包含任何独立信息**
2. **所有服务信息均引用项目文档**
3. **确保信息的一致性和准确性**
4. **避免维护多份重复信息**

---
**维护原则**: 严格遵循单一源头原则，所有详细信息请参考项目文档。
```

## ❌ 错误做法

### 禁止在微服务README中包含
- 独立的服务功能描述
- 重复的技术栈列表
- 详细的API端点说明
- 完整的部署步骤
- 测试用例详情

### ❌ 错误示例
```markdown
# 错误：包含重复内容

## 📋 服务概述
EVCS认证授权服务是系统的安全核心，负责：
- 用户身份认证和授权  ❌ 重复内容
- JWT令牌的生成和验证  ❌ 重复内容

## 🔧 技术栈
- Spring Boot 3.2.10  ❌ 重复内容
- Spring Security 6.x   ❌ 重复内容
```

## 🎯 实施效果

### ✅ 优势
1. **信息一致性**: 避免多处信息不一致
2. **维护效率**: 只需更新一处
3. **文档准确性**: 减少过时信息
4. **简化管理**: 明确信息职责

### 📊 质量提升
- 消除重复内容
- 确保信息权威性
- 提高文档可维护性
- 降低维护成本

## 🔗 引用标准

### 文档锚点命名规范
```markdown
# 服务章节
{service-name} → evcs-auth, evcs-gateway, evcs-station

# 技术栈章节
技术栈 → #技术栈

# 配置章节
{service-name}-config → evcs-auth-config
```

### 引用格式标准
```markdown
[描述](../../docs/path/file.md#anchor)
```

## 📋 检查清单

### 创建README时检查
- [ ] 是否包含任何独立信息？
- [ ] 所有信息是否都引用项目文档？
- [ ] 引用链接是否正确？
- [ ] 是否遵循单一源头原则？

### 维护README时检查
- [ ] 更新信息时是否更新源头文档？
- [ ] 是否避免了在README中直接修改内容？
- [ ] 引用链接是否仍然有效？

---

**最后更新**: 2025-11-07 21:55
**维护者**: 架构团队
**状态**: 已发布