# 测试环境部署指南（指针）

> 一句话说明：测试环境相关的“部署/运行”文档统一入口（仅做索引，权威内容见链接）。

**最后更新**: 2026-01-12  \
**维护者**: DevOps 团队  \
**状态**: 已发布

---

## 选择入口

### 1) 本地/CI 测试环境（Docker Compose）

- 测试分层与运行方式：见 [docs/testing/UNIFIED-TESTING-GUIDE.md](../testing/UNIFIED-TESTING-GUIDE.md)
- Compose 编排入口：见 [docs/deployment/DEPLOYMENT-WORKFLOW.md](DEPLOYMENT-WORKFLOW.md)

### 2) K3s 内部测试环境（K8s，无外网）

发布流程与验收基线：见 [docs/deployment/DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md) 中的「K3S 内部测试环境（无外网）发布流程（推荐）」。

关键脚本（SSOT）：

- 后端（Jib 推镜像）：`./gradlew pushK8sImages`
- 前端（本机有 Docker）：[k8s/push-images-from-local.sh](../../k8s/push-images-from-local.sh)
- 前端（WSL 本机无 Docker）：[k8s/build-admin-frontend-prebuilt-from-local.sh](../../k8s/build-admin-frontend-prebuilt-from-local.sh)
- 应用部署：`bash k8s/deploy.sh`
