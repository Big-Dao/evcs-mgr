# 文档去重与索引收敛总结（2025-12-17）

> 一句话说明：本次变更聚焦“全仓库非归档文档去重”，通过建立统一索引（SSOT）并将非权威文档指针化，修复断链与 Markdown lint 阻塞。

**最后更新**: 2025-12-17  
**维护者**: 技术负责人 / DevOps 团队  
**状态**: 已归档

---

## 目标

- 将重复的“文档导航/部署命令/测试环境说明”等内容收敛为单一入口（SSOT）。
- 将非权威文档改为指针或引用，减少重复维护与内容漂移。
- 修复断链与 Markdown lint 阻塞，保证非归档文档可持续维护。

## 关键产出（SSOT）

- 全局文档总索引：[docs/DOCUMENTATION-INDEX.md](../../DOCUMENTATION-INDEX.md)
- docs 入口页：[docs/README.md](../../README.md)
- 部署索引：[docs/deployment/README.md](../../deployment/README.md)

## 处理策略

- 对“入口/索引类”文档：保留短内容 + 指向 SSOT。
- 对“重复说明类”文档：改为指针，并将历史细节下沉至 `docs/archive/`。
- 对“易过时清单”（端口表、目录树、长命令列表）：优先引用权威文档或代码文件（如 `settings.gradle`）。

## 校验

- `docs/` 目录范围的错误检查：无 Markdown lint 错误。

## 备注

- 归档区保留历史快照，允许存在重复；非归档区以 [docs/DOCUMENTATION-INDEX.md](../../DOCUMENTATION-INDEX.md) 为准。
