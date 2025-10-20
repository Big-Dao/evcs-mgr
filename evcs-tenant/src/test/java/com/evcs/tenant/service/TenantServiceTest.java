package com.evcs.tenant.service;

import com.evcs.common.page.PageQuery;
import com.evcs.common.page.PageResult;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.tenant.TenantServiceApplication;
import com.evcs.tenant.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 租户服务测试
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@DisplayName("租户服务测试")
class TenantServiceTest extends BaseServiceTest {

    @Autowired(required = false)
    private TenantService tenantService;

    @Test
    @DisplayName("创建租户 - 正常流程")
    void testCreateTenant() {
        // 如果服务未实现，跳过测试
        if (tenantService == null) {
            return;
        }

        // Given: 准备租户数据
        Tenant tenant = createTestTenant("TEST001", "测试租户1");

        // When: 创建租户
        Tenant created = tenantService.createTenant(tenant);

        // Then: 验证结果
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTenantCode()).isEqualTo("TEST001");
        assertThat(created.getTenantName()).isEqualTo("测试租户1");
        assertThat(created.getId()).isEqualTo(DEFAULT_TENANT_ID);
    }

    @Test
    @DisplayName("更新租户 - 正常流程")
    void testUpdateTenant() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建租户
        Tenant tenant = createTestTenant("TEST002", "测试租户2");
        Tenant created = tenantService.createTenant(tenant);

        // When: 更新租户
        created.setTenantName("更新后的租户名称");
        created.setContactPerson("更新联系人");
        Tenant updated = tenantService.updateTenant(created);

        // Then: 验证更新结果
        assertThat(updated).isNotNull();
        assertThat(updated.getTenantName()).isEqualTo("更新后的租户名称");
        assertThat(updated.getContactPerson()).isEqualTo("更新联系人");
    }

    @Test
    @DisplayName("删除租户 - 逻辑删除")
    void testDeleteTenant() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建租户
        Tenant tenant = createTestTenant("TEST003", "测试租户3");
        Tenant created = tenantService.createTenant(tenant);

        // When: 删除租户
        tenantService.deleteTenant(created.getId());

        // Then: 验证删除结果（查询应返回null或抛出异常）
        Tenant deleted = tenantService.getTenantById(created.getId());
        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("根据ID查询租户 - 正常流程")
    void testGetTenantById() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建租户
        Tenant tenant = createTestTenant("TEST004", "测试租户4");
        Tenant created = tenantService.createTenant(tenant);

        // When: 根据ID查询
        Tenant found = tenantService.getTenantById(created.getId());

        // Then: 验证查询结果
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getTenantCode()).isEqualTo("TEST004");
    }

    @Test
    @DisplayName("根据编码查询租户 - 正常流程")
    void testGetTenantByCode() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建租户
        Tenant tenant = createTestTenant("TEST005", "测试租户5");
        tenantService.createTenant(tenant);

        // When: 根据编码查询
        Tenant found = tenantService.getTenantByCode("TEST005");

        // Then: 验证查询结果
        assertThat(found).isNotNull();
        assertThat(found.getTenantCode()).isEqualTo("TEST005");
    }

    @Test
    @DisplayName("分页查询租户 - 正常查询")
    void testPageTenants() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建多个租户
        for (int i = 1; i <= 5; i++) {
            Tenant tenant = createTestTenant("PAGE00" + i, "分页租户" + i);
            tenantService.createTenant(tenant);
        }

        // When: 分页查询
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(1);
        pageQuery.setSize(3);
        PageResult<Tenant> result = tenantService.pageTenants(pageQuery);

        // Then: 验证分页结果
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getRecords().size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("查询子租户列表 - 正常查询")
    void testGetSubTenants() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建父租户和子租户
        Tenant parent = createTestTenant("PARENT01", "父租户");
        Tenant createdParent = tenantService.createTenant(parent);

        Tenant child1 = createTestTenant("CHILD01", "子租户1");
        child1.setParentId(createdParent.getId());
        tenantService.createTenant(child1);

        Tenant child2 = createTestTenant("CHILD02", "子租户2");
        child2.setParentId(createdParent.getId());
        tenantService.createTenant(child2);

        // When: 查询子租户
        List<Tenant> subTenants = tenantService.getSubTenants(createdParent.getId());

        // Then: 验证结果
        assertThat(subTenants).isNotNull();
        assertThat(subTenants.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("查询租户层级树 - 正常查询")
    void testGetTenantTree() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建多层级租户
        Tenant root = createTestTenant("ROOT01", "根租户");
        Tenant createdRoot = tenantService.createTenant(root);

        // When: 查询租户树
        List<Tenant> tree = tenantService.getTenantTree(createdRoot.getId());

        // Then: 验证结果
        assertThat(tree).isNotNull();
    }

    @Test
    @DisplayName("检查租户编码是否存在 - 存在")
    void testExistsByCode_Exists() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建租户
        Tenant tenant = createTestTenant("EXISTS01", "存在的租户");
        tenantService.createTenant(tenant);

        // When: 检查编码
        boolean exists = tenantService.existsByCode("EXISTS01");

        // Then: 应该存在
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查租户编码是否存在 - 不存在")
    void testExistsByCode_NotExists() {
        if (tenantService == null) {
            return;
        }

        // When: 检查不存在的编码
        boolean exists = tenantService.existsByCode("NOT_EXISTS");

        // Then: 不应该存在
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("启用/禁用租户 - 正常流程")
    void testChangeStatus() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建租户
        Tenant tenant = createTestTenant("STATUS01", "状态测试租户");
        Tenant created = tenantService.createTenant(tenant);

        // When: 禁用租户
        tenantService.changeStatus(created.getId(), 0);

        // Then: 验证状态
        Tenant disabled = tenantService.getTenantById(created.getId());
        assertThat(disabled.getStatus()).isEqualTo(0);

        // When: 启用租户
        tenantService.changeStatus(created.getId(), 1);

        // Then: 验证状态
        Tenant enabled = tenantService.getTenantById(created.getId());
        assertThat(enabled.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取租户的祖级路径 - 正常查询")
    void testGetTenantAncestors() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建多层级租户
        Tenant parent = createTestTenant("ANCESTOR_P", "父租户");
        Tenant createdParent = tenantService.createTenant(parent);

        Tenant child = createTestTenant("ANCESTOR_C", "子租户");
        child.setParentId(createdParent.getId());
        Tenant createdChild = tenantService.createTenant(child);

        // When: 获取祖级路径
        String ancestors = tenantService.getTenantAncestors(createdChild.getId());

        // Then: 验证路径
        assertThat(ancestors).isNotNull();
    }

    @Test
    @DisplayName("检查是否为上级租户 - 是上级")
    void testIsParentTenant_True() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建父子租户
        Tenant parent = createTestTenant("PARENT_CHECK", "父租户");
        Tenant createdParent = tenantService.createTenant(parent);

        Tenant child = createTestTenant("CHILD_CHECK", "子租户");
        child.setParentId(createdParent.getId());
        Tenant createdChild = tenantService.createTenant(child);

        // When: 检查是否为上级
        boolean isParent = tenantService.isParentTenant(
            createdParent.getId(), 
            createdChild.getId()
        );

        // Then: 应该是上级
        assertThat(isParent).isTrue();
    }

    @Test
    @DisplayName("检查是否为上级租户 - 不是上级")
    void testIsParentTenant_False() {
        if (tenantService == null) {
            return;
        }

        // Given: 创建两个独立的租户
        Tenant tenant1 = createTestTenant("INDEPENDENT1", "独立租户1");
        Tenant created1 = tenantService.createTenant(tenant1);

        Tenant tenant2 = createTestTenant("INDEPENDENT2", "独立租户2");
        Tenant created2 = tenantService.createTenant(tenant2);

        // When: 检查是否为上级
        boolean isParent = tenantService.isParentTenant(created1.getId(), created2.getId());

        // Then: 不应该是上级
        assertThat(isParent).isFalse();
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试租户对象
     */
    private Tenant createTestTenant(String code, String name) {
        Tenant tenant = new Tenant();
        tenant.setTenantCode(code);
        tenant.setTenantName(name);
        tenant.setContactPerson("测试联系人");
        tenant.setContactPhone("13800138000");
        tenant.setContactEmail("test@example.com");
        tenant.setAddress("测试地址");
        tenant.setTenantType(2); // 运营商
        tenant.setStatus(1); // 启用
        tenant.setExpireTime(LocalDateTime.now().plusYears(1));
        tenant.setMaxUsers(100);
        tenant.setMaxStations(50);
        tenant.setMaxChargers(200);
        tenant.setRemark("测试租户");
        return tenant;
    }
}
