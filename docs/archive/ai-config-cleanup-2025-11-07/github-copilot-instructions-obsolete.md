# EVCS充电桩管理系统 - GitHub Copilot使用指南

## 🤖 GitHub Copilot专用配置

### 📋 项目上下文
请首先阅读共享的项目上下文：[SHARED-PROJECT-CONTEXT.md](../.ai-assistant-config/SHARED-PROJECT-CONTEXT.md)

该文档包含完整的项目架构、微服务模块、技术栈和编码标准。

### 🔧 GitHub Copilot使用方式

#### 1. 自动集成
GitHub Copilot会自动读取.github/copilot-instructions.md文件，无需额外配置。

#### 2. IDE集成
- **VS Code**: 安装GitHub Copilot扩展
- **JetBrains IDEs**: 安装GitHub Copilot插件
- **Neovim**: 配置GitHub Copilot插件

#### 3. 项目启动
```bash
# 克隆项目
git clone [repository-url]
cd evcs-mgr

# 启动开发环境
docker-compose -f docker-compose.core-dev.yml up -d

# 在IDE中打开项目
# Copilot会自动加载项目上下文
```

### 🎯 GitHub Copilot特有能力

#### 智能代码生成
- 基于项目上下文的智能补全
- 自动遵循微服务架构模式
- 智能生成测试代码

#### 多语言支持
- Java (主要开发语言)
- SQL (数据库脚本)
- YAML/JSON (配置文件)
- Shell脚本 (部署脚本)

#### 上下文感知
- 理解项目结构和依赖关系
- 识别微服务边界和接口
- 遵循项目的命名规范

### 📝 GitHub Copilot使用技巧

#### 1. 有效注释提示
```java
// 创建充电站服务类，包含创建、查询、更新、删除方法
@Service
public class StationService {
    // Copilot会根据注释生成完整实现
}
```

#### 2. 方法级提示
```java
// 根据充电站ID和状态查询可用充电桩
public List<ChargingPoleDTO> getAvailablePoles(Long stationId, PoleStatus status) {
    // Copilot会生成具体实现
}
```

#### 3. 测试代码生成
```java
// 为StationService创建单元测试
@ExtendWith(MockitoExtension.class)
class StationServiceTest {
    // Copilot会生成完整的测试类
}
```

#### 4. 配置文件生成
```yaml
# application-dev.yml
# Copilot会根据项目配置生成开发环境配置
```

### 🚀 GitHub Copilot最佳实践

#### 1. 项目级提示
在项目根目录添加文件级注释：
```java
/*
 * EVCS充电站管理系统
 * 微服务架构：Spring Boot + Java 21
 * 数据库：PostgreSQL + Redis
 * 缓存：Spring Cache
 * 安全：JWT + RBAC
 */
```

#### 2. 类级提示
```java
/**
 * 充电站管理服务
 * 功能：充电站的CRUD操作、状态管理、设备控制
 * 缓存：使用Redis缓存充电站信息
 * 权限：需要STATION_MANAGE权限
 */
@Service
```

#### 3. 方法级提示
```java
/**
 * 创建新的充电站
 * @param request 充电站创建请求
 * @return 创建的充电站信息
 * @throws BusinessException 充电站名称重复时抛出
 * @CacheEvict 创建完成后清除缓存
 */
```

### 🔍 Copilot Chat高级用法

#### 1. 代码解释
```
请解释这段代码的业务逻辑：
[粘贴代码]
```

#### 2. 代码优化
```
请优化这段代码的性能：
[粘贴代码]
```

#### 3. 错误修复
```
这段代码有编译错误，请帮我修复：
[粘贴代码]
```

#### 4. 架构建议
```
我需要实现一个新的支付模块，请给我架构建议
```

### 🚨 GitHub Copilot限制

#### 1. 上下文限制
- 单次对话的上下文窗口有限
- 建议分模块进行开发
- 使用清晰的文件结构

#### 2. 实时信息
- 无法访问运行时环境
- 不能执行实际代码
- 建议结合本地测试

#### 3. 复杂业务逻辑
- 对于复杂业务逻辑可能理解不准确
- 建议提供详细的注释和说明
- 分步骤实现复杂功能

### 📊 质量保证

#### 1. 代码审查
- Copilot生成的代码需要人工审查
- 检查是否符合项目规范
- 验证业务逻辑的正确性

#### 2. 测试验证
- 为生成的代码编写单元测试
- 进行集成测试验证
- 确保代码质量和安全性

#### 3. 性能测试
- 关注生成的代码性能
- 避免N+1查询问题
- 合理使用缓存机制

### 📚 相关资源

#### 官方文档
- [GitHub Copilot文档](https://docs.github.com/en/copilot)
- [Copilot Chat使用指南](https://docs.github.com/en/copilot/copilot-chat)

#### 项目文档
- [统一部署指南](../../DEPLOYMENT-GUIDE.md)
- [API设计规范](../../docs/02-development/API-DESIGN-STANDARDS.md)
- [代码质量检查清单](../../docs/02-development/CODE-QUALITY-CHECKLIST.md)

#### 开发工具
- [统一测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md)
- [数据库设计规范](../../docs/02-development/DATABASE-DESIGN-STANDARDS.md)
- [Docker配置指南](../../DOCKER-CONFIGURATION-GUIDE.md)

---

**通过遵循本配置和共享项目上下文，GitHub Copilot可以为EVCS项目提供智能化的代码生成和建议。**