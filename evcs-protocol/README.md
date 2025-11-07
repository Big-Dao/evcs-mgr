# EVCS协议处理服务

> **端口**: 8085 | **状态**: 活跃 | **负责人**: 协议团队

## 🔗 完整文档

| 文档类型 | 链接 |
|---------|------|
| **📖 服务功能** | [架构文档](../../docs/architecture/architecture.md#evcs-protocol) |
| **🔧 开发指南** | [开发指南](../../docs/development/DEVELOPER-GUIDE.md#evcs-protocol) |
| **📡 API接口** | [API文档](../../docs/references/API-DOCUMENTATION.md#evcs-protocol) |
| **🚀 部署配置** | [部署指南](../../docs/deployment/DEPLOYMENT-GUIDE.md#evcs-protocol) |
| **🧪 测试指南** | [测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md#evcs-protocol) |
| **📊 监控运维** | [监控指南](../../docs/operations/MONITORING-GUIDE.md#evcs-protocol) |

## ⚡ 快速验证
```bash
# 健康检查
curl http://localhost:8085/actuator/health

# 构建和运行
./gradlew :evcs-protocol:bootRun
```

---
**维护原则**: 本README仅包含端口、状态、负责人信息，所有详细内容请参考项目文档。
