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
@SpringBootTest(classes = { TenantServiceApplication.class, com.evcs.tenant.config.TestConfig.class })
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
        SysTenant update = new SysTenant();
        update.setId(tenant.getId());
        update.setTenantName("更新后的系统租户");
        update.setContactPerson("更新后的联系人");
        boolean result = sysTenantService.updateTenant(update);

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
        // Given: 使用已有的运营商租户上下文创建“子租户”，以符合层级过滤规则
        switchTenant(2L);
        SysTenant tenant = createTestSysTenant("SEARCH001", "可搜索的租户");
        tenant.setParentId(2L);
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
                newParent.getId());

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
                null);

        // Then: 应该存在
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查租户编码是否存在 - 编码不存在")
    void testCheckTenantCodeExists_CodeNotExists() {
        // When: 检查不存在的编码
        boolean exists = sysTenantService.checkTenantCodeExists(
                "NOT_EXISTS_CODE",
                null);

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
                tenant.getId());

        // Then: 不应该认为存在（因为排除了自身）
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("多租户隔离 - 不同租户的数据应该隔离")
    void testTenantIsolation() {
        // Given: 使用两套已存在的租户上下文（2/3）分别创建各自的“子租户”数据
        switchTenant(2L);
        SysTenant tenant2 = createTestSysTenant("TENANT2_DATA", "租户2的数据");
        tenant2.setParentId(2L);
        sysTenantService.saveTenant(tenant2);

        switchTenant(3L);
        SysTenant tenant3 = createTestSysTenant("TENANT3_DATA", "租户3的数据");
        tenant3.setParentId(3L);
        sysTenantService.saveTenant(tenant3);

        // 此处 sysTenantService.queryTenantPage 已实现了代码级的租户过滤

        // When: 租户2查询
        switchTenant(2L);
        Page<SysTenant> page1 = new Page<>(1, 100);
        IPage<SysTenant> result1 = sysTenantService.queryTenantPage(page1, new SysTenant());

        // Then: 租户2只能看到自己的数据
        List<SysTenant> list1 = result1.getRecords();
        assertThat(list1).isNotEmpty();
        assertThat(list1.stream().anyMatch(t -> "TENANT2_DATA".equals(t.getTenantCode()))).isTrue();
        assertThat(list1.stream().anyMatch(t -> "TENANT3_DATA".equals(t.getTenantCode()))).isFalse();

        // When: 租户3查询
        switchTenant(3L);
        Page<SysTenant> page2 = new Page<>(1, 100);
        IPage<SysTenant> result2 = sysTenantService.queryTenantPage(page2, new SysTenant());

        // Then: 租户3只能看到自己的数据
        List<SysTenant> list2 = result2.getRecords();
        assertThat(list2).isNotEmpty();
        assertThat(list2.stream().anyMatch(t -> "TENANT3_DATA".equals(t.getTenantCode()))).isTrue();
        assertThat(list2.stream().anyMatch(t -> "TENANT2_DATA".equals(t.getTenantCode()))).isFalse();
    }

    @Test
    @DisplayName("修改租户状态 - 递归禁用验证")
    void testChangeStatus_RecursiveDisable() {
        // Given: 创建三层租户结构 (父 -> 子 -> 孙)，初始全启用
        SysTenant parent = createTestSysTenant("REC_PARENT", "递归父租户");
        parent.setStatus(1);
        sysTenantService.saveTenant(parent);

        SysTenant child = createTestSysTenant("REC_CHILD", "递归子租户");
        child.setParentId(parent.getId());
        child.setStatus(1);
        sysTenantService.saveTenant(child);

        SysTenant grandChild = createTestSysTenant("REC_GCHILD", "递归孙租户");
        grandChild.setParentId(child.getId());
        grandChild.setStatus(1);
        sysTenantService.saveTenant(grandChild);

        // When: 禁用父租户
        boolean result = sysTenantService.changeStatus(parent.getId(), 0);

        // Then: 验证所有后代都被禁用
        assertThat(result).isTrue();

        SysTenant p = sysTenantService.getTenantById(parent.getId());
        assertThat(p.getStatus()).isEqualTo(0);

        SysTenant c = sysTenantService.getTenantById(child.getId());
        assertThat(c.getStatus()).isEqualTo(0);

        SysTenant gc = sysTenantService.getTenantById(grandChild.getId());
        assertThat(gc.getStatus()).isEqualTo(0);
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

    @Test
    @DisplayName("能力边界管控 - 配额修改限制验证")
    void testCapabilityBoundary_Quota() {
        // Given: 创建 父 -> 子 租户
        SysTenant parent = createTestSysTenant("PARENT_CAP", "上级租户");
        parent.setStatus(1); // 激活
        sysTenantService.saveTenant(parent);

        // 切换到父租户上下文创建子租户
        switchTenant(parent.getId());
        SysTenant child = createTestSysTenant("CHILD_CAP", "下级租户");
        child.setParentId(parent.getId());
        child.setMaxUsers(10);
        child.setMaxStations(5);
        child.setStatus(1);
        sysTenantService.saveTenant(child);

        // Scenario 1: Self update quota (Should Fail)
        switchTenant(child.getId());
        SysTenant updateSelf = new SysTenant();
        updateSelf.setId(child.getId());
        updateSelf.setMaxUsers(100);

        try {
            sysTenantService.updateTenant(updateSelf);
            throw new RuntimeException("Should have failed but success");
        } catch (RuntimeException e) {
            // Expected
        }

        // Scenario 2: Parent update quota (Should Success)
        switchTenant(parent.getId());
        SysTenant updateByParent = new SysTenant();
        updateByParent.setId(child.getId());
        updateByParent.setMaxUsers(20);

        boolean success = sysTenantService.updateTenant(updateByParent);
        assertThat(success).isTrue();

        // 验证修改成功
        SysTenant updated = sysTenantService.getTenantById(child.getId());
        assertThat(updated.getMaxUsers()).isEqualTo(20);
    }
}
