# 按枪口（Connector）控制 - 测试报告

> 一句话说明：记录“按枪口启停（仅落库会话字段）+ 管理后台枪口启停 UI”相关的最小可执行验证结果。

**最后更新**: 2025-12-21  \
**维护者**: 平台研发组  \
**状态**: 已发布

---

## 1. 覆盖范围

- 后端（evcs-station）：按枪口 start/stop 接口落库逻辑
- 前端（evcs-admin）：枪口表格展示 + 行级启动/停止交互

## 2. 已验证项

### 2.1 evcs-station（后端）

- 用例：`com.evcs.station.controller.ChargerConnectorSessionControllerTest`
- 运行方式（示例）：

```bash
./gradlew :evcs-station:test --tests "com.evcs.station.controller.ChargerConnectorSessionControllerTest"
```

- 结果：✅ BUILD SUCCESSFUL（2025-12-21）

### 2.2 evcs-admin（前端）

- 校验：TypeScript 类型检查 + 生产构建
- 运行方式（示例）：

```bash
cd evcs-admin
npm run build
```

- 结果：✅ 构建成功（2025-12-21）

## 3. 未包含项

- `:evcs-station:bootRun` 本地启动验证：不在本报告范围内（若启动失败，需单独定位环境依赖/配置问题）
- 真实协议启停（RemoteStart/RemoteStop）：当前实现未对接协议侧控制，故不在验证范围内

