# 极简微服务README模板
 
> **端口**: {port} | **状态**: {active|development} | **负责人**: {team}

## 🔗 完整文档

| 文档类型 | 链接 |
|---------|------|
| **📖 服务功能** | [架构文档](../../docs/architecture/architecture.md#{service-name}) |
| **🔧 开发指南** | [开发指南](../../docs/development/DEVELOPER-GUIDE.md#{service-name}) |
| **📡 API接口** | [API文档](../../docs/references/API-DOCUMENTATION.md#{service-name}) |
| **🚀 部署配置** | [部署指南](../../docs/deployment/DEPLOYMENT-GUIDE.md#{service-name}) |
| **🧪 测试指南** | [测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md#{service-name}) |
| **📊 监控运维** | [监控指南](../../docs/operations/MONITORING-GUIDE.md#{service-name}) |

## ⚡ 快速验证
```bash
# 健康检查
curl http://localhost:{port}/actuator/health

# 构建和运行
./gradlew :{service-name}:bootRun
```

---

**维护原则**: 本README仅包含端口、状态、负责人信息，所有详细内容请参考项目文档。
