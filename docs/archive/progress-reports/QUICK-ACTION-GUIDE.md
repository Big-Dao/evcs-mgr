# 🚀 快速行动指南

## ✅ 今日已完成
- **IDE 错误**: 3908 → 0 ✅
- **测试执行**: 100+ 测试全部通过 ✅
- **覆盖率基线**: 30.2% ✅

---

## 🎯 下一步：提升测试覆盖率

### 目标
**本周**: 30.2% → 50%  
**最终**: 80%

### 优先补充模块
1. 🔴 **evcs-tenant** (0% → 50%) - 最急需
2. 🔴 **evcs-protocol** (13% → 50%)
3. 🔴 **evcs-integration** (24% → 50%)
4. 🔴 **evcs-station** (34% → 60%)
5. 🔴 **evcs-order** (35% → 60%)

---

## 📝 今天开始

### 任务 1: 补充 evcs-tenant 测试

创建测试文件：
```bash
# 1. TenantService 测试
evcs-tenant/src/test/java/com/evcs/tenant/service/TenantServiceTest.java

# 2. SysTenantService 测试  
evcs-tenant/src/test/java/com/evcs/tenant/service/SysTenantServiceTest.java

# 3. Controller 测试
evcs-tenant/src/test/java/com/evcs/tenant/controller/TenantControllerTest.java
```

### 使用测试模板
```java
@SpringBootTest
@Import(BaseServiceTest.class)
class TenantServiceTest {
    @Autowired
    private TestDataFactory factory;
    
    @Autowired
    private TenantTestHelper helper;
    
    @Test
    void shouldCreateTenant() {
        // 测试代码
    }
}
```

---

## 🛠️ 常用命令

```powershell
# 运行测试
./gradlew :evcs-tenant:test

# 生成覆盖率
./gradlew :evcs-tenant:test jacocoTestReport

# 查看覆盖率
.\scripts\check-coverage.ps1

# 打开报告
Invoke-Item evcs-tenant\build\reports\jacoco\index.html
```

---

## 📚 参考文档
- 详细进度: `NEXT-STEP-PROGRESS.md`
- 测试框架: `TEST-FRAMEWORK-SUMMARY.md`
- IDE 配置: `IDE-FIX-GUIDE.md`

---

**目标**: 今天完成 evcs-tenant 测试，覆盖率提升到 35%
