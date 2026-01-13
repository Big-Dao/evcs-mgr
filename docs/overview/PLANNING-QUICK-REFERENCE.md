# 规划快速参考

> **简短描述**：面向全员的“下一步计划”快速入口与可验证的状态快照。

**最后更新**: 2026-01-13  \
**维护者**: 技术负责人/架构团队  \
**状态**: 已发布  \

**完整文档**: [下一步行动计划](./NEXT-PLAN.md)

---

## 1. 当前状态（可验证口径）

本节仅给出“可从代码库直接验证”的快照，不在此维护运行态结论（如线上完成度、覆盖率、K8s UP 数量等）。

### 1.1 代码库快照（统计口径）

以下数字来自仓库扫描统计，用于快速感知规模；以“如何验证”的命令输出为准。

- Gradle 模块（settings.gradle include）：12 个
- Java 源文件：main 367 个、test 82 个
- Spring 注解粗略统计：`@RestController` 58、`@Service` 97

### 1.2 如何验证（本地命令）

```bash
# 模块列表（以 settings.gradle / Gradle 为准）
./gradlew -q projects

# Java 文件数（按文件计数）
find . -type f -name '*.java' | grep '/src/main/java/' | wc -l
find . -type f -name '*.java' | grep '/src/test/java/' | wc -l

# 注解数量（粗略口径：按行匹配注解出现次数）
grep -R --line-number --fixed-string '@RestController' --include='*.java' --exclude-dir=build --exclude-dir=bin . | grep '/src/main/java/' | wc -l
grep -R --line-number --fixed-string '@Service' --include='*.java' --exclude-dir=build --exclude-dir=bin . | grep '/src/main/java/' | wc -l
```

---

## 2. 下一步重点（4周计划）

### 2.1 Week 1-2: 关键功能完善
- 支付对账功能（8个TODO）
- 协议异常处理（3个TODO）
- 监控健康检查（2个TODO）

### 2.2 Week 3: 代码质量提升
- 测试模板实现（9个TODO）
- 缓存预热优化（1个TODO）

### 2.3 Week 4: 性能优化与监控
- 性能基线建立
- 监控体系完善

---

## 3. 关键指标（建议跟踪口径）

本节仅定义“建议跟踪的指标维度”，具体数值应来自 CI、测试平台或监控系统，不在本文硬编码。

| 指标 | 当前 | 目标 | 状态 |
|------|------|------|------|
| TODO/缺陷清理 | 以 Issue/PR/看板为准 | 完成核心功能 | 持续推进 |
| 测试覆盖率 | 以 CI 报告为准 | 90%+ | 持续推进 |
| 性能基线 | 以性能测试与监控为准 | 已建立 | 持续推进 |

---

## 4. 立即行动

1. **查看完整计划**: [NEXT-PLAN.md](./NEXT-PLAN.md)
2. **任务跟踪**: 以 Issue/PR/看板为准（避免在 Markdown 中重复维护可变清单）
3. **开始执行**: 从 P0 任务开始，按优先级推进

---

> 需要更详细的拆解与验收口径，请直接打开完整计划文档。