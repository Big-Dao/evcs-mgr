# 文档结构重构总结（2026-01-12）

> 一句话说明：建立服务标识/端口的设计 SSOT，收敛运维侧重复文档与指针文档，降低文档漂移风险并提升正式度。

**最后更新**: 2026-01-12  \
**维护者**: 技术负责人  \
**状态**: 已发布

---

## 1. 关键结果

1.1. 单一来源（SSOT）落地

- 新增设计 SSOT：`docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md`。
- 文档主标识统一为 Docker Compose service key，并明确 Compose/K8s/Spring 的映射关系。

1.2. 减少碎片化文档

- 删除 3 份“指针/重复映射”类文档，入口信息合并到统一索引。

1.3. 提升文档正式度

- 清理非必要 emoji（优先处理标题与提示区）。

---

## 2. 变更明细

2.1. 新增

- `docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md`
- `docs/overview/CLEANUP-PLAN-2026-01-12.md`
- `docs/overview/CLEANUP-SUMMARY-2026-01-12.md`

2.2. 更新

- `docs/DOCUMENTATION-INDEX.md`
- `docs/operations/SERVICES-REFERENCE.md`
- `docs/operations/MONITORING-GUIDE.md`
- `docs/operations/DEFAULT-CREDENTIALS.md`

2.3. 删除

- `docs/operations/SERVICE_NAMES_MAPPING.md`
- `docs/operations/PROJECT-STRUCTURE.md`
- `docs/overview/QUICK-DOCUMENTATION-GUIDE.md`

---

## 3. 后续建议（不阻塞）

- 将规划类文档（overview 下多个计划/进度文档）进一步聚合，减少重复状态快照。
- 对非档案区剩余 emoji-heavy 文档进行分批次格式规范化（仅在内容变更时顺带处理）。

---

## 4. 文档历史

4.1. 2026-01-12

- 完成本次重构。
