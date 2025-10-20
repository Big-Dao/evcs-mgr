package com.evcs.tenant.service;

import cn.hutool.core.lang.tree.Tree;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.tenant.TenantServiceApplication;
import com.evcs.tenant.entity.SysTenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统租户服务测试
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@DisplayName("系统租户服务测试")
class SysTenantServiceImplTest extends BaseServiceTest {

    @Autowired
    private ISysTenantService sysTenantService;

    @Test
    @DisplayName("保存租户 - 正常流程")
    void testSaveTenant() {
        // Given: 准备租户数据
        SysTenant tenant = createTestSysTenant("SYS_TEST001", "系统测试租户1");

        // When: 保存租户
        boolean result = sysTenantService.saveTenant(tenant);

        // Then: 验证结果
        assertThat(result).isTrue();
        assertThat(tenant.getId()).isNotNull();
    }

    @Test
    @DisplayName("更新租户 - 正常流程")
    void testUpdateTenant() {
        // Given: 创建租户
        SysTenant tenant = createTestSysTenant("SYS_TEST002", "系统测试租户2");
        sysTenantService.saveTenant(tenant);

        // When: 更新租户
        tenant.setTenantName("更新后的系统租户");
        tenant.setContactPerson("更新后的联系人");
        boolean result = sysTenantService.updateTenant(tenant);

        // Then: 验证更新
        assertThat(result).isTrue();
        SysTenant updated = sysTenantService.getTenantById(tenant.getId());
        assertThat(updated.getTenantName()).isEqualTo("更新后的系统租户");
        assertThat(updated.getContactPerson()).isEqualTo("更新后的联系人");
    }

    @Test
    @DisplayName("删除租户 - 逻辑删除")
    void testDeleteTenant() {
        // Given: 创建租户
        SysTenant tenant = createTestSysTenant("SYS_TEST003", "系统测试租户3");
        sysTenantService.saveTenant(tenant);
        Long tenantId = tenant.getId();

        // When: 删除租户
        boolean result = sysTenantService.deleteTenant(tenantId);

        // Then: 验证删除
        assertThat(result).isTrue();
        SysTenant deleted = sysTenantService.getTenantById(tenantId);
        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("根据ID查询租户 - 正常查询")
    void testGetTenantById() {
        // Given: 创建租户
        SysTenant tenant = createTestSysTenant("SYS_TEST004", "系统测试租户4");
        sysTenantService.saveTenant(tenant);

        // When: 根据ID查询
        SysTenant found = sysTenantService.getTenantById(tenant.getId());

        // Then: 验证查询结果
        assertThat(found).isNotNull();
        assertThat(found.getTenantCode()).isEqualTo("SYS_TEST004");
        assertThat(found.getTenantName()).isEqualTo("系统测试租户4");
    }

    @Test
    @DisplayName("分页查询租户 - 无条件查询")
    void testQueryTenantPage_NoCondition() {
        // Given: 创建多个租户
        for (int i = 1; i <= 5; i++) {
            SysTenant tenant = createTestSysTenant("PAGE_SYS_00" + i, "分页系统租户" + i);
            sysTenantService.saveTenant(tenant);
        }

        // When: 分页查询
        Page<SysTenant> page = new Page<>(1, 3);
        IPage<SysTenant> result = sysTenantService.queryTenantPage(page, new SysTenant());

        // Then: 验证分页结果
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isNotEmpty();
        assertThat(result.getSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("分页查询租户 - 按名称查询")
    void testQueryTenantPage_WithName() {
        // Given: 创建租户
        SysTenant tenant = createTestSysTenant("SEARCH001", "可搜索的租户");
        sysTenantService.saveTenant(tenant);

        // When: 按名称查询
        Page<SysTenant> page = new Page<>(1, 10);
        SysTenant query = new SysTenant();
        query.setTenantName("可搜索");
        IPage<SysTenant> result = sysTenantService.queryTenantPage(page, query);

        // Then: 验证查询结果
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isNotEmpty();
    }

    @Test
    @DisplayName("查询租户树 - 正常查询")
    void testGetTenantTree() {
        // Given: 创建多层级租户
        SysTenant parent = createTestSysTenant("TREE_PARENT", "树形父租户");
        sysTenantService.saveTenant(parent);

        SysTenant child1 = createTestSysTenant("TREE_CHILD1", "树形子租户1");
        child1.setParentId(parent.getId());
        sysTenantService.saveTenant(child1);

        SysTenant child2 = createTestSysTenant("TREE_CHILD2", "树形子租户2");
        child2.setParentId(parent.getId());
        sysTenantService.saveTenant(child2);

        // When: 查询租户树
        List<Tree<Long>> tree = sysTenantService.getTenantTree();

        // Then: 验证树结构
        assertThat(tree).isNotNull();
        assertThat(tree).isNotEmpty();
    }

    @Test
    @DisplayName("查询子租户列表 - 正常查询")
    void testGetSubTenants() {
        // Given: 创建父子租户
        SysTenant parent = createTestSysTenant("SUB_PARENT", "父租户");
        sysTenantService.saveTenant(parent);

        SysTenant child1 = createTestSysTenant("SUB_CHILD1", "子租户1");
        child1.setParentId(parent.getId());
        sysTenantService.saveTenant(child1);

        SysTenant child2 = createTestSysTenant("SUB_CHILD2", "子租户2");
        child2.setParentId(parent.getId());
        sysTenantService.saveTenant(child2);

        // When: 查询子租户
        List<SysTenant> subTenants = sysTenantService.getSubTenants(parent.getId());

        // Then: 验证结果
        assertThat(subTenants).isNotNull();
        assertThat(subTenants.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("查询租户子节点ID列表 - 正常查询")
    void testGetTenantChildren() {
        // Given: 创建多层级租户
        SysTenant parent = createTestSysTenant("CHILDREN_P", "父租户");
        sysTenantService.saveTenant(parent);

        SysTenant child = createTestSysTenant("CHILDREN_C", "子租户");
        child.setParentId(parent.getId());
        sysTenantService.saveTenant(child);

        SysTenant grandChild = createTestSysTenant("CHILDREN_GC", "孙租户");
        grandChild.setParentId(child.getId());
        sysTenantService.saveTenant(grandChild);

        // When: 查询所有子节点
        List<Long> childrenIds = sysTenantService.getTenantChildren(parent.getId());

        // Then: 验证结果应包含子节点和孙节点
        assertThat(childrenIds).isNotNull();
        assertThat(childrenIds.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("移动租户 - 正常流程")
    void testMoveTenant() {
        // Given: 创建源父租户、目标父租户和子租户
        SysTenant oldParent = createTestSysTenant("OLD_PARENT", "旧父租户");
        sysTenantService.saveTenant(oldParent);

        SysTenant newParent = createTestSysTenant("NEW_PARENT", "新父租户");
        sysTenantService.saveTenant(newParent);

        SysTenant child = createTestSysTenant("MOVE_CHILD", "待移动子租户");
        child.setParentId(oldParent.getId());
        sysTenantService.saveTenant(child);

        // When: 移动租户
        boolean result = sysTenantService.moveTenant(
            child.getId(), 
            newParent.getId()
        );

        // Then: 验证移动结果
        assertThat(result).isTrue();
        SysTenant moved = sysTenantService.getTenantById(child.getId());
        assertThat(moved.getParentId()).isEqualTo(newParent.getId());
    }

    @Test
    @DisplayName("修改租户状态 - 启用到禁用")
    void testChangeStatus_EnableToDisable() {
        // Given: 创建启用状态的租户
        SysTenant tenant = createTestSysTenant("STATUS_TEST1", "状态测试租户1");
        tenant.setStatus(1);
        sysTenantService.saveTenant(tenant);

        // When: 禁用租户
        boolean result = sysTenantService.changeStatus(tenant.getId(), 0);

        // Then: 验证状态变更
        assertThat(result).isTrue();
        SysTenant updated = sysTenantService.getTenantById(tenant.getId());
        assertThat(updated.getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("修改租户状态 - 禁用到启用")
    void testChangeStatus_DisableToEnable() {
        // Given: 创建禁用状态的租户
        SysTenant tenant = createTestSysTenant("STATUS_TEST2", "状态测试租户2");
        tenant.setStatus(0);
        sysTenantService.saveTenant(tenant);

        // When: 启用租户
        boolean result = sysTenantService.changeStatus(tenant.getId(), 1);

        // Then: 验证状态变更
        assertThat(result).isTrue();
        SysTenant updated = sysTenantService.getTenantById(tenant.getId());
        assertThat(updated.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("检查租户编码是否存在 - 编码存在")
    void testCheckTenantCodeExists_CodeExists() {
        // Given: 创建租户
        SysTenant tenant = createTestSysTenant("CHECK_EXISTS", "编码检查租户");
        sysTenantService.saveTenant(tenant);

        // When: 检查相同编码
        boolean exists = sysTenantService.checkTenantCodeExists(
            "CHECK_EXISTS", 
            null
        );

        // Then: 应该存在
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查租户编码是否存在 - 编码不存在")
    void testCheckTenantCodeExists_CodeNotExists() {
        // When: 检查不存在的编码
        boolean exists = sysTenantService.checkTenantCodeExists(
            "NOT_EXISTS_CODE", 
            null
        );

        // Then: 不应该存在
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("检查租户编码是否存在 - 排除自身")
    void testCheckTenantCodeExists_ExcludeSelf() {
        // Given: 创建租户
        SysTenant tenant = createTestSysTenant("EXCLUDE_SELF", "排除自身租户");
        sysTenantService.saveTenant(tenant);

        // When: 检查编码时排除自身
        boolean exists = sysTenantService.checkTenantCodeExists(
            "EXCLUDE_SELF", 
            tenant.getId()
        );

        // Then: 不应该认为存在（因为排除了自身）
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("多租户隔离 - 不同租户的数据应该隔离")
    void testTenantIsolation() {
        // Given: 租户1创建数据
        switchTenant(1L);
        SysTenant tenant1 = createTestSysTenant("TENANT1_DATA", "租户1的数据");
        sysTenantService.saveTenant(tenant1);

        // 租户2创建数据
        switchTenant(2L);
        SysTenant tenant2 = createTestSysTenant("TENANT2_DATA", "租户2的数据");
        sysTenantService.saveTenant(tenant2);

        // When: 租户1查询
        switchTenant(1L);
        Page<SysTenant> page1 = new Page<>(1, 100);
        IPage<SysTenant> result1 = sysTenantService.queryTenantPage(page1, new SysTenant());

        // Then: 租户1不应该看到租户2的数据
        assertThat(result1.getRecords()).isNotNull();
        boolean hasTenant2Data = result1.getRecords().stream()
            .anyMatch(t -> "TENANT2_DATA".equals(t.getTenantCode()));
        assertThat(hasTenant2Data).isFalse();

        // When: 租户2查询
        switchTenant(2L);
        Page<SysTenant> page2 = new Page<>(1, 100);
        IPage<SysTenant> result2 = sysTenantService.queryTenantPage(page2, new SysTenant());

        // Then: 租户2不应该看到租户1的数据
        assertThat(result2.getRecords()).isNotNull();
        boolean hasTenant1Data = result2.getRecords().stream()
            .anyMatch(t -> "TENANT1_DATA".equals(t.getTenantCode()));
        assertThat(hasTenant1Data).isFalse();
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试系统租户对象
     */
    private SysTenant createTestSysTenant(String code, String name) {
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(code);
        tenant.setTenantName(name);
        tenant.setContactPerson("系统测试联系人");
        tenant.setContactPhone("13900139000");
        tenant.setContactEmail("systest@example.com");
        tenant.setAddress("系统测试地址");
        tenant.setSocialCode("91110000000000000X");
        tenant.setTenantType(2); // 运营商
        tenant.setStatus(1); // 启用
        tenant.setExpireTime(LocalDateTime.now().plusYears(1));
        tenant.setMaxUsers(100);
        tenant.setMaxStations(50);
        tenant.setMaxChargers(200);
        tenant.setRemark("系统测试租户");
        return tenant;
    }
}
