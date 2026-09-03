package com.evcs.auth.controller.internal;

import com.evcs.auth.entity.SysUser;
import com.evcs.auth.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户统计内部端点测试（tenant 仪表盘数据来源）。
 */
@SpringBootTest(classes = com.evcs.auth.AuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "evcs.internal.api.enabled=true",
        "evcs.internal.api.token=auth-internal-test-token-0123456789",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("用户统计内部端点")
class UserStatsInternalTest {

    @Autowired
    private MockMvc mockMvc;

    @Resource
    private SysUserMapper sysUserMapper;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        com.evcs.common.tenant.TenantContext.clear();
    }

    private void saveUser(String username, Long tenantId) {
        com.evcs.common.tenant.TenantContext.setCurrentTenantId(tenantId);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    @Test
    @DisplayName("活跃用户计数 - 只统计启用用户且按租户集合过滤")
    void shouldCountActiveUsersByTenants() throws Exception {
        saveUser("STATS-U1", 1L);
        saveUser("STATS-U2", 1L);
        saveUser("STATS-U3", 2L);

        com.evcs.common.tenant.TenantContext.clear();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/internal/api/v1/stats/users/active-count")
                        .header("X-Internal-Token", "auth-internal-test-token-0123456789")
                        .param("tenantIds", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    @DisplayName("缺少内部令牌 - 应拒绝")
    void shouldRejectWithoutInternalToken() throws Exception {
        com.evcs.common.tenant.TenantContext.clear();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/internal/api/v1/stats/users/active-count").param("tenantIds", "1"))
                .andExpect(status().isUnauthorized());
    }
}
