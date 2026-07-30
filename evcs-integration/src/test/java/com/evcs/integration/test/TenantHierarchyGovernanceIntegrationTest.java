package com.evcs.integration.test;

import com.evcs.common.tenant.HierarchyValidator;
import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.controller.TenantController;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.ITenantAuditLogService;
import com.evcs.tenant.service.ISysTenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        classes = {TenantTestApplication.class},
        properties = {"spring.main.allow-circular-references=true"},
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@DisplayName("多层级租户治理 Part 2 - 集成测试")
class TenantHierarchyGovernanceIntegrationTest {

    private static final Long TENANT_ROOT = 100L;
    private static final Long TENANT_CHILD = 200L;
    private static final Long TENANT_GRANDCHILD = 300L;

    @Resource
    private SysTenantMapper tenantMapper;

    @Resource
    private ISysTenantService tenantService;

    @Resource
    private ITenantAuditLogService auditLogService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TenantController tenantController;

    @Resource
    private HierarchyValidator hierarchyValidator;

    @Resource(name = "chargingExecutor")
    private ThreadPoolTaskExecutor chargingExecutor;

    @BeforeEach
    void setUp() {
        // 设置安全认证上下文，满足方法级 @PreAuthorize 要求
        var authorities = AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_TENANT_ADMIN");
        var authentication = new UsernamePasswordAuthenticationToken("test-admin", "N/A", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        jdbcTemplate.execute("DELETE FROM tenant_audit_log");
        jdbcTemplate.execute("DELETE FROM sys_tenant");

        insertTenant(TENANT_ROOT, "ROOT", "平台租户", 0L, "0", 1);
        insertTenant(TENANT_CHILD, "CHILD", "子租户", TENANT_ROOT, "0,100", 2);
        insertTenant(TENANT_GRANDCHILD, "GRANDCHILD", "孙租户", TENANT_CHILD, "0,100,200", 2);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("跨层只读：上级访问下级应允许")
    void shouldAllowCrossLayerReadForDescendants() {
        TenantContext.setTenantId(TENANT_ROOT);
        TenantContext.setUserId(1L);

        boolean allowed = hierarchyValidator.validateAccess(TENANT_ROOT, TENANT_CHILD);
        assertTrue(allowed, "上级租户应允许访问下级租户");

        allowed = hierarchyValidator.validateAccess(TENANT_ROOT, TENANT_GRANDCHILD);
        assertTrue(allowed, "上级租户应允许访问孙级租户");
    }

    @Test
    @DisplayName("越权拒绝：下级访问上级应禁止")
    void shouldDenyUpwardAccess() {
        TenantContext.setTenantId(TENANT_CHILD);
        TenantContext.setUserId(2L);

        boolean allowed = hierarchyValidator.validateAccess(TENANT_CHILD, TENANT_ROOT);
        assertFalse(allowed, "下级租户不应访问上级租户数据");
    }

    @Test
    @DisplayName("禁用联动：禁用上级租户应递归禁用所有下级并记录审计")
    void shouldDisableRecursivelyAndWriteAuditLog() {
        TenantContext.setTenantId(TENANT_ROOT);
        TenantContext.setUserId(10L);

        boolean updated = tenantService.changeStatus(TENANT_ROOT, 0);
        assertTrue(updated, "禁用上级租户应成功");

        assertEquals(0, tenantService.getTenantById(TENANT_ROOT).getStatus());
        assertEquals(0, tenantService.getTenantById(TENANT_CHILD).getStatus());
        assertEquals(0, tenantService.getTenantById(TENANT_GRANDCHILD).getStatus());

        long auditCount = auditLogService.lambdaQuery()
                .eq(com.evcs.tenant.entity.TenantAuditLog::getAction, "DISABLE_RECURSIVE")
                .eq(com.evcs.tenant.entity.TenantAuditLog::getTargetTenantId, TENANT_ROOT)
                .count();
        assertTrue(auditCount >= 1, "递归禁用应记录审计日志");
    }

    @Test
    @DisplayName("异步上下文传播：线程池任务应继承并清理 TenantContext")
    void shouldPropagateTenantContextInAsyncExecutor() throws Exception {
        TenantContext.setTenantId(TENANT_CHILD);
        TenantContext.setUserId(22L);

        Future<Long> tenantResult = chargingExecutor.submit(TenantContext::getTenantId);
        Future<Long> userResult = chargingExecutor.submit(TenantContext::getUserId);

        assertEquals(TENANT_CHILD, tenantResult.get(), "异步任务应继承租户ID");
        assertEquals(22L, userResult.get(), "异步任务应继承用户ID");

        TenantContext.clear();
        Future<Long> clearedTenant = chargingExecutor.submit(TenantContext::getTenantId);
        assertNull(clearedTenant.get(), "清理上下文后异步任务不应残留租户ID");
    }

    @Test
    @DisplayName("跨层只读：上级读取下级子租户列表")
    void shouldReadDescendantListWithHierarchyScope() {
        TenantContext.setTenantId(TENANT_ROOT);
        TenantContext.setUserId(3L);

        var result = tenantController.getSubTenants(TENANT_CHILD);
        assertNotNull(result.getData(), "应返回下级租户列表");
        assertEquals(1, result.getData().size(), "子租户应只有一个");
        assertEquals(TENANT_GRANDCHILD, result.getData().get(0).getId());
    }

    private void insertTenant(Long id, String code, String name, Long parentId, String ancestors, int type) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setTenantId(id);
        tenant.setTenantCode(code);
        tenant.setTenantName(name);
        tenant.setParentId(parentId);
        tenant.setAncestors(ancestors);
        tenant.setTenantType(type);
        tenant.setStatus(1);
        tenant.setContactPerson("tester");
        tenant.setContactPhone("13800138000");
        tenant.setContactEmail("tenant@example.com");
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        tenantMapper.insert(tenant);
    }
}
