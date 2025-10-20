package com.evcs.tenant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.evcs.common.result.Result;
import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.tenant.TenantServiceApplication;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.service.ISysTenantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 租户控制器测试
 */
@SpringBootTest(classes = TenantServiceApplication.class)
@AutoConfigureMockMvc
@DisplayName("租户控制器测试")
class TenantControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ISysTenantService sysTenantService;

    @Test
    @DisplayName("创建租户 - 返回成功")
    void testCreateTenant() throws Exception {
        // Given: 准备租户数据
        String requestBody = """
            {
                "tenantCode": "CTRL_TEST001",
                "tenantName": "控制器测试租户1",
                "contactPerson": "测试联系人",
                "contactPhone": "13800138000",
                "contactEmail": "test@example.com",
                "tenantType": 2,
                "status": 1
            }
            """;

        // When & Then: 发送请求并验证响应
        mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("租户创建成功"))
                .andExpect(jsonPath("$.data.tenantCode").value("CTRL_TEST001"));
    }

    @Test
    @DisplayName("更新租户 - 返回成功")
    void testUpdateTenant() throws Exception {
        // Given: 创建租户
        SysTenant tenant = createAndSaveTenant("CTRL_TEST002", "控制器测试租户2");

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 更新租户
        String updateBody = """
            {
                "tenantCode": "CTRL_TEST002",
                "tenantName": "更新后的租户名称",
                "contactPerson": "更新后的联系人",
                "contactPhone": "13900139000",
                "contactEmail": "updated@example.com",
                "tenantType": 2
            }
            """;

        mockMvc.perform(put("/tenant/" + tenant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("租户更新成功"));
    }

    @Test
    @DisplayName("删除租户 - 返回成功")
    void testDeleteTenant() throws Exception {
        // Given: 创建租户
        SysTenant tenant = createAndSaveTenant("CTRL_TEST003", "控制器测试租户3");

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 删除租户
        mockMvc.perform(delete("/tenant/" + tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("租户删除成功"));
    }

    @Test
    @DisplayName("根据ID查询租户 - 返回成功")
    void testGetTenant() throws Exception {
        // Given: 创建租户
        SysTenant tenant = createAndSaveTenant("CTRL_TEST004", "控制器测试租户4");

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 查询租户
        mockMvc.perform(get("/tenant/" + tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenantCode").value("CTRL_TEST004"))
                .andExpect(jsonPath("$.data.tenantName").value("控制器测试租户4"));
    }

    @Test
    @DisplayName("分页查询租户 - 返回成功")
    void testPageTenants() throws Exception {
        // Given: 创建多个租户
        for (int i = 1; i <= 5; i++) {
            createAndSaveTenant("PAGE_CTRL_00" + i, "分页控制器租户" + i);
        }

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 分页查询
        mockMvc.perform(get("/tenant/page")
                .param("page", "1")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("查询子租户列表 - 返回成功")
    void testGetSubTenants() throws Exception {
        // Given: 创建父子租户
        SysTenant parent = createAndSaveTenant("SUB_CTRL_P", "父租户");

        SysTenant child1 = createTestTenant("SUB_CTRL_C1", "子租户1");
        child1.setParentId(parent.getId());
        sysTenantService.saveTenant(child1);

        SysTenant child2 = createTestTenant("SUB_CTRL_C2", "子租户2");
        child2.setParentId(parent.getId());
        sysTenantService.saveTenant(child2);

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 查询子租户
        mockMvc.perform(get("/tenant/" + parent.getId() + "/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("查询租户树 - 返回成功")
    void testGetTenantTree() throws Exception {
        // Given: 创建多层级租户
        SysTenant root = createAndSaveTenant("TREE_CTRL", "树根租户");

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 查询租户树
        mockMvc.perform(get("/tenant/tree")
                .param("rootId", root.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("启用/禁用租户 - 返回成功")
    void testChangeStatus() throws Exception {
        // Given: 创建租户
        SysTenant tenant = createAndSaveTenant("STATUS_CTRL", "状态控制器租户");

        // When & Then: 禁用租户
        mockMvc.perform(put("/tenant/" + tenant.getId() + "/status")
                .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("状态修改成功"));

        // Verify: 验证状态已更改
        SysTenant updated = sysTenantService.getTenantById(tenant.getId());
        assertThat(updated.getStatus()).isEqualTo(0);

        // 重新设置租户上下文（MockMvc 请求后会被拦截器清空）
        setUpTenantContext();
        
        // When & Then: 启用租户
        mockMvc.perform(put("/tenant/" + tenant.getId() + "/status")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify: 验证状态已更改
        updated = sysTenantService.getTenantById(tenant.getId());
        assertThat(updated.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("检查租户编码 - 编码存在")
    void testCheckTenantCode_Exists() throws Exception {
        // Given: 创建租户
        createAndSaveTenant("CHECK_CODE_CTRL", "编码检查租户");

        // When & Then: 检查编码
        mockMvc.perform(get("/tenant/check-code")
                .param("tenantCode", "CHECK_CODE_CTRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("检查租户编码 - 编码不存在")
    void testCheckTenantCode_NotExists() throws Exception {
        // When & Then: 检查不存在的编码
        mockMvc.perform(get("/tenant/check-code")
                .param("tenantCode", "NOT_EXISTS_CODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("创建租户 - 缺少必填字段应返回错误")
    void testCreateTenant_MissingRequiredFields() throws Exception {
        // Given: 缺少必填字段的请求
        String requestBody = """
            {
                "tenantCode": "INVALID_TEST"
            }
            """;

        // When & Then: 应该返回验证错误
        mockMvc.perform(post("/tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建并保存测试租户
     */
    private SysTenant createAndSaveTenant(String code, String name) {
        SysTenant tenant = createTestTenant(code, name);
        sysTenantService.saveTenant(tenant);
        return tenant;
    }

    /**
     * 创建测试租户对象（不保存）
     */
    private SysTenant createTestTenant(String code, String name) {
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(code);
        tenant.setTenantName(name);
        tenant.setContactPerson("控制器测试联系人");
        tenant.setContactPhone("13800138000");
        tenant.setContactEmail("ctrl@example.com");
        tenant.setAddress("控制器测试地址");
        tenant.setTenantType(2); // 运营商
        tenant.setStatus(1); // 启用
        tenant.setExpireTime(LocalDateTime.now().plusYears(1));
        tenant.setMaxUsers(100);
        tenant.setMaxStations(50);
        tenant.setMaxChargers(200);
        tenant.setRemark("控制器测试租户");
        return tenant;
    }
}
