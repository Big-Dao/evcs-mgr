# EVCS Manager 项目文档中心

> **文档导航** | 快速找到你需要的信息

---

## 📖 文档分类

### 🎯 项目规划类

#### 当前规划
- **[PROGRESS.md](./PROGRESS.md)** ⭐ **从这里开始！**
  - 当前状态、已完成里程碑、下一步行动
  - 每周更新，实时反映项目进展
  - 适合：所有团队成员快速了解当前状态

- **[下一步行动计划.md](./下一步行动计划.md)** 📋 **详细行动计划**
  - 优先级任务清单、验收标准、时间规划
  - 风险管理、交付物清单
  - 适合：开发团队、项目经理深度阅读

- **[下一步计划速览.md](./下一步计划速览.md)** 📄 **快速参考**
  - 浓缩版，核心要点一目了然
  - 优先级排序、成功标准、主要风险
  - 适合：全体成员快速查阅

#### 历史规划与参考
- **[ROADMAP.md](./ROADMAP.md)** - 项目路线图（P0-P4阶段）
- **[DEVELOPMENT-PLAN.md](./DEVELOPMENT-PLAN.md)** - P3阶段详细开发计划（参考）
- **[P3阶段规划总览.md](./P3阶段规划总览.md)** - P3阶段总览（参考）
- **[P3阶段甘特图.md](./P3阶段甘特图.md)** - P3时间线可视化（参考）
- **[P3每周行动清单.md](./P3每周行动清单.md)** - 周任务执行清单（参考）
- **[CHANGELOG.md](./CHANGELOG.md)** - 版本变更日志
- **[archive/](./archive/)** - 已完成阶段的文档归档

---

### 🔧 技术文档类

#### 核心技术文档
- **[TECHNICAL-DESIGN.md](./TECHNICAL-DESIGN.md)** 🏗️ **技术方案设计**
  - 架构总览、多租户设计、分时电价算法
  - 订单闭环、可观测性、可扩展点
  - 适合：新成员了解技术架构

- **[DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md)** 👨‍💻 **开发者指南**
  - 新模块开发流程、代码规范
  - 多租户开发注意事项、Git工作流
  - 适合：开发人员日常参考

- **[API-DOCUMENTATION.md](./API-DOCUMENTATION.md)** 📡 **API文档**
  - 核心接口请求/响应示例
  - 错误码说明、调用时序图
  - 适合：前后端对接、接口测试

#### 运维与测试
- **[DEPLOYMENT-GUIDE.md](./DEPLOYMENT-GUIDE.md)** 🚀 **部署指南**
  - Docker Compose / Kubernetes部署
  - 配置参数说明、故障排查
  - 适合：运维部署、环境配置

- **[OPERATIONS-MANUAL.md](./OPERATIONS-MANUAL.md)** 🔧 **运维手册**
  - 监控、日志、备份恢复
  - 故障排查、性能优化、应急响应
  - 适合：运维人员日常运维

- **[MONITORING-GUIDE.md](./MONITORING-GUIDE.md)** 📊 **监控指南**
  - Prometheus + Grafana配置
  - 关键指标、告警规则
  - 适合：监控体系建设

- **[TESTING-GUIDE.md](./TESTING-GUIDE.md)** 🧪 **测试指南**
  - 单元测试、集成测试、端到端测试
  - 测试框架和最佳实践
  - 适合：测试工程师、QA

#### 架构专题
- **[TENANT_ISOLATION_COMPARISON.md](./TENANT_ISOLATION_COMPARISON.md)** 🔒 **多租户方案对比**
  - Schema隔离 vs 行级隔离对比分析
  - 适合：理解多租户架构设计

- **[TenantIsolationDemo.java](./TenantIsolationDemo.java)** 💡 **多租户演示代码**
  - 代码示例，展示多租户隔离效果

- **[协议对接指南.md](./协议对接指南.md)** 🔌 **协议对接指南**
  - OCPP和云快充协议对接
  - 适合：协议开发、设备对接

#### 产品需求
- **[PRODUCT-REQUIREMENTS.md](./PRODUCT-REQUIREMENTS.md)** 📋 **产品需求文档（PRD）**
  - 业务需求、功能清单、用户故事
  - 适合：了解产品定位和功能范围

---

---

## 🗂️ 文档使用指南

### 按角色查找文档

#### 🎯 项目经理
**每周工作流**：
1. **周一**: 查看 [PROGRESS.md](./PROGRESS.md) 了解当前状态
2. **每日**: 查看 [下一步行动计划.md](./下一步行动计划.md) 跟踪进度
3. **周五**: 更新 [PROGRESS.md](./PROGRESS.md)，总结工作
4. **月度**: 评审 [ROADMAP.md](./ROADMAP.md) 调整规划

**关键文档**：
- [PROGRESS.md](./PROGRESS.md)
- [下一步行动计划.md](./下一步行动计划.md)
- [ROADMAP.md](./ROADMAP.md)

#### 💻 开发工程师
**开发工作流**：
1. **开发前**: 查看 [TECHNICAL-DESIGN.md](./TECHNICAL-DESIGN.md) 了解技术方案
2. **开发中**: 参考 [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) 遵循规范
3. **遇到问题**: 查看 [OPERATIONS-MANUAL.md](./OPERATIONS-MANUAL.md) 故障排查
4. **提交代码**: 确保符合 [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) 代码规范

**关键文档**：
- [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md)
- [TECHNICAL-DESIGN.md](./TECHNICAL-DESIGN.md)
- [API-DOCUMENTATION.md](./API-DOCUMENTATION.md)
- [TENANT_ISOLATION_COMPARISON.md](./TENANT_ISOLATION_COMPARISON.md)

#### 🧪 测试工程师
**测试工作流**：
1. **测试计划**: 查看 [TESTING-GUIDE.md](./TESTING-GUIDE.md)
2. **功能测试**: 参考 [PRODUCT-REQUIREMENTS.md](./PRODUCT-REQUIREMENTS.md)
3. **接口测试**: 参考 [API-DOCUMENTATION.md](./API-DOCUMENTATION.md)
4. **回归测试**: 执行完整测试套件

**关键文档**：
- [TESTING-GUIDE.md](./TESTING-GUIDE.md)
- [PRODUCT-REQUIREMENTS.md](./PRODUCT-REQUIREMENTS.md)
- [API-DOCUMENTATION.md](./API-DOCUMENTATION.md)

#### 🔧 DevOps工程师
**运维工作流**：
1. **部署**: 查看 [DEPLOYMENT-GUIDE.md](./DEPLOYMENT-GUIDE.md)
2. **监控**: 参考 [MONITORING-GUIDE.md](./MONITORING-GUIDE.md)
3. **运维**: 查看 [OPERATIONS-MANUAL.md](./OPERATIONS-MANUAL.md)
4. **故障排查**: 参考运维手册故障排查章节

**关键文档**：
- [DEPLOYMENT-GUIDE.md](./DEPLOYMENT-GUIDE.md)
- [MONITORING-GUIDE.md](./MONITORING-GUIDE.md)
- [OPERATIONS-MANUAL.md](./OPERATIONS-MANUAL.md)

#### 👤 新成员
**入职必读**：
1. **第1天**: [README.md](../README.md) - 项目总览
2. **第2天**: [TECHNICAL-DESIGN.md](./TECHNICAL-DESIGN.md) - 技术架构
3. **第3天**: [PROGRESS.md](./PROGRESS.md) - 当前进度与规划
4. **第4天**: [TENANT_ISOLATION_COMPARISON.md](./TENANT_ISOLATION_COMPARISON.md) - 多租户架构
5. **第5天**: [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) - 开发指南，开始参与开发

---

### 按场景查找文档

#### 📅 规划与进度
- 想了解当前状态 → [PROGRESS.md](./PROGRESS.md)
- 想看详细行动计划 → [下一步行动计划.md](./下一步行动计划.md)
- 想快速了解要点 → [下一步计划速览.md](./下一步计划速览.md)
- 想了解长期规划 → [ROADMAP.md](./ROADMAP.md)

#### 💻 开发与实现
- 想了解技术架构 → [TECHNICAL-DESIGN.md](./TECHNICAL-DESIGN.md)
- 想了解开发规范 → [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md)
- 想了解多租户实现 → [TENANT_ISOLATION_COMPARISON.md](./TENANT_ISOLATION_COMPARISON.md)
- 想看代码示例 → [TenantIsolationDemo.java](./TenantIsolationDemo.java)
- 想了解API接口 → [API-DOCUMENTATION.md](./API-DOCUMENTATION.md)

#### 🚀 部署与运维
- 想部署系统 → [DEPLOYMENT-GUIDE.md](./DEPLOYMENT-GUIDE.md)
- 想配置监控 → [MONITORING-GUIDE.md](./MONITORING-GUIDE.md)
- 想了解运维 → [OPERATIONS-MANUAL.md](./OPERATIONS-MANUAL.md)
- 想进行测试 → [TESTING-GUIDE.md](./TESTING-GUIDE.md)

#### 📦 产品与需求
- 想了解产品功能 → [PRODUCT-REQUIREMENTS.md](./PRODUCT-REQUIREMENTS.md)
- 想了解版本历史 → [CHANGELOG.md](./CHANGELOG.md)
- 想了解未来规划 → [ROADMAP.md](./ROADMAP.md)
- 想了解协议对接 → [协议对接指南.md](./协议对接指南.md)

---

## 📊 文档统计

| 类别 | 文档数量 | 说明 |
|------|---------|------|
| 规划类 | 7个 | 当前规划 + 历史参考 |
| 技术类 | 8个 | 架构、开发、API、协议 |
| 运维类 | 4个 | 部署、监控、运维、测试 |
| 产品类 | 2个 | PRD、路线图 |
| 归档类 | 20+个 | 已完成阶段的文档 |

---

## 🔄 文档更新规则

### 更新频率
- **实时更新**: [PROGRESS.md](./PROGRESS.md)（每周五）
- **阶段更新**: [ROADMAP.md](./ROADMAP.md)（每个阶段结束）
- **版本更新**: [CHANGELOG.md](./CHANGELOG.md)（每次版本发布）
- **按需更新**: 技术文档（架构变更时）

### 更新流程
1. 修改文档
2. 提交PR
3. Code Review
4. 合并到main分支

### 文档规范
- 使用Markdown格式
- 中文文档优先
- 包含目录（长文档）
- 包含最后更新时间

---

## 📞 文档相关问题

### 如何反馈文档问题？
1. **提Issue**: 在GitHub上提Issue
2. **提PR**: 直接修改文档提交PR
3. **联系文档维护者**: 项目经理

### 如何贡献文档？
1. Fork仓库
2. 创建分支 `docs/xxx`
3. 修改文档
4. 提交PR
5. 等待Review

### 文档维护者
- **项目经理**: 规划类文档
- **技术负责人**: 技术类文档
- **QA负责人**: 问题类文档

---

## 📚 相关资源

### 外部文档
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Vue 3 官方文档](https://vuejs.org/)
- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)

### 内部资源
- [项目主README](../README.md)
- [代码仓库](https://github.com/Big-Dao/evcs-mgr)
- [Wiki页面]（待创建）

---

## 📝 文档版本

| 日期 | 版本 | 主要变更 |
|------|------|---------|
| 2025-01-08 | v1.0 | 创建文档中心，添加P3阶段完整规划 |
| 2025-10-12 | v2.0 | 重大更新：归档已完成阶段文档，更新文档索引结构 |

---

**最后更新**: 2025-10-12  
**维护者**: 项目经理  
**反馈**: 请提Issue或PR
