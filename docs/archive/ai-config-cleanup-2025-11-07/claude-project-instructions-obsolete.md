# EVCS项目Claude Code使用指令

## 🤖 Claude Code专用配置

### 📋 项目上下文
请首先阅读共享的项目上下文：[SHARED-PROJECT-CONTEXT.md](../.ai-assistant-config/SHARED-PROJECT-CONTEXT.md)

该文档包含完整的项目架构、微服务模块、技术栈和编码标准。

### 🔧 Claude Code使用方式

#### 1. 自动读取配置
Claude Code会自动读取.claude/目录下的配置文件，无需手动配置。

#### 2. 项目启动
```bash
# 推荐使用核心开发环境
docker-compose -f docker-compose.core-dev.yml up -d

# 检查服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 查看服务日志
docker-compose -f docker-compose.core-dev.yml logs -f
```

#### 3. 开发工作流
1. **优先参考现有实现**: 在生成代码前，先查看项目中类似的实现
2. **保持一致性**: 与现有代码风格、命名规范保持一致
3. **包含测试**: 为新功能生成对应的单元测试
4. **添加文档**: 为公共API生成JavaDoc注释

### 🎯 Claude Code特有能力

#### 智能代码补全
- 基于项目上下文的智能补全
- 自动遵循项目的编码规范
- 支持微服务架构的智能建议

#### 项目理解
- 自动分析项目结构和依赖关系
- 理解微服务间的调用关系
- 识别架构模式和设计原则

#### 代码质量检查
- 实时代码质量分析
- 自动检测潜在问题
- 建议最佳实践改进

### 📝 Claude Code使用技巧

#### 1. 上下文管理
```bash
# Claude Code会自动读取以下文件
.claude/project-instructions.md     # 本配置文件
.claude/sprint-instructions.md      # 当前冲刺目标
PROJECT-CODING-STANDARDS.md         # 项目编码标准
```

#### 2. 有效提示
```
# 好的提示示例
请为充电站管理模块创建StationService类，遵循项目微服务架构和编码标准。

# 包含具体要求的提示
请创建一个订单支付接口，包含支付宝和微信支付，确保符合项目的安全要求。
```

#### 3. 代码审查模式
```
请审查以下代码，检查是否符合EVCS项目的架构规范和质量标准：
[粘贴代码]
```

### 🚨 Claude Code限制

#### 1. 不能直接执行代码
- Claude Code只能生成和建议代码
- 需要开发者手动执行和测试
- 建议使用IDE的运行功能

#### 2. 上下文窗口限制
- 大型项目可能需要分批处理
- 优先关注当前修改的文件
- 使用清晰的模块化结构

#### 3. 实时信息限制
- 无法获取运行时信息
- 建议结合日志和调试工具
- 使用项目文档作为参考

### 📚 相关资源

#### 项目文档
- [统一部署指南](../../DEPLOYMENT-GUIDE.md)
- [Docker配置指南](../../DOCKER-CONFIGURATION-GUIDE.md)
- [AI助手规范索引](../../AI-ASSISTANTS-INDEX.md)

#### 开发工具
- [统一测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md)
- [API设计规范](../../docs/02-development/API-DESIGN-STANDARDS.md)
- [数据库设计规范](../../docs/02-development/DATABASE-DESIGN-STANDARDS.md)

---

**通过遵循本配置和共享项目上下文，Claude Code可以为EVCS项目生成高质量、符合架构规范的代码。**