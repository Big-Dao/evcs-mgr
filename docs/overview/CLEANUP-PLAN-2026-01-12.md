# 文档结构重构计划（2026-01-12）

> 一句话说明：通过建立设计层 SSOT、收敛运维侧引用、删除碎片指针文档，降低重复与漂移风险，使文档更正式、易读。

**最后更新**: 2026-01-12  \
**维护者**: 技术负责人  \
**状态**: 已执行

---

## 1. 目标

1.1. 优化篇幅颗粒度

- 将“服务命名/端口/跨环境标识映射”集中到设计文档 SSOT。
- 删除重复指针文档，将必要入口信息合并到统一索引。

1.2. 核心内容前置

- 新增/重构文档均采用“先使用方式/权威来源，后演进历史”的结构。

1.3. 文档更正式

- 清理标题/目录中的 emoji。
- 正文中的 emoji 若无强调必要则移除。

---

## 2. 范围与约束

2.1. `docs/archive/**` 保持不动

- 归档区视为历史快照，不做修链与内容调整。

2.2. 服务主标识口径

- 文档主标识统一使用 Docker Compose 的 service key。

---

## 3. 变更清单

3.1. 新增

- `docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md`：服务标识与端口规范（SSOT）。

3.2. 更新

- `docs/DOCUMENTATION-INDEX.md`：新增 SSOT 入口，移除被删除文档链接，采用编号章节。
- `docs/operations/SERVICES-REFERENCE.md`：补充权威来源引用，清理标题 emoji。
- `docs/operations/MONITORING-GUIDE.md`：清理标题/提示 emoji。
- `docs/operations/DEFAULT-CREDENTIALS.md`：清理标题/提示 emoji。

3.3. 删除（Option B）

- `docs/operations/SERVICE_NAMES_MAPPING.md`
- `docs/operations/PROJECT-STRUCTURE.md`
- `docs/overview/QUICK-DOCUMENTATION-GUIDE.md`

---

## 4. 验收标准

- `docs/DOCUMENTATION-INDEX.md` 不包含对已删除文档的引用。
- `docs/archive/**` 未发生变更。
- 服务标识与端口映射的权威来源为设计 SSOT 文档，运维文档不再重复维护映射表。

---

## 5. 文档历史

5.1. 2026-01-12

- 创建本计划并执行对应重构。 
