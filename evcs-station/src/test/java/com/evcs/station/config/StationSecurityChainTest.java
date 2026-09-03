package com.evcs.station.config;

import com.evcs.common.test.base.BaseControllerTest;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Station 服务安全链激活测试。
 *
 * <p>历史问题：{@link StationSecurityConfig} 的 SecurityFilterChain 方法缺少 @Bean，
 * 整条安全链从未注册，实际生效的是 Spring Boot 默认链（全部 401）。
 * 本测试锁定基线要求的行为：白名单放行、JWT 认证生效、
 * 仅凭 X-User-Id 请求头不再伪造认证、内部端点由内部令牌保护。
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class,
        com.evcs.station.config.TestConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "evcs.internal.api.enabled=true",
        "evcs.internal.api.token=station-internal-test-token-0123456789"
})
@AutoConfigureMockMvc
@DisplayName("Station 安全链")
class StationSecurityChainTest extends BaseControllerTest {

    @Autowired
    private JwtUtil jwtUtil;

    private void clearPredefinedAuth() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("健康检查在白名单内 - 无认证应放行到端点本身（不被 401/403 拦截）")
    void healthShouldBeAccessibleWithoutAuth() throws Exception {
        clearPredefinedAuth();

        // 测试环境部分健康指标为 DOWN，端点自身返回 503；
        // 关键断言是请求穿过了安全链（非 401/403），而不是指标状态
        MvcResult result = mockMvc.perform(get("/actuator/health")).andReturn();

        assertNotEquals(401, result.getResponse().getStatus(), "健康检查不应被认证拦截");
        assertNotEquals(403, result.getResponse().getStatus(), "健康检查不应被授权拦截");
    }

    @Test
    @DisplayName("有效 JWT - 应通过链内过滤器认证（不再 401）")
    void validJwtShouldAuthenticate() throws Exception {
        clearPredefinedAuth();
        String token = jwtUtil.generateToken(1L, "admin", 1L, List.of("ADMIN"));

        MvcResult result = mockMvc.perform(get("/charger/page")
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andReturn();

        assertNotEquals(401, result.getResponse().getStatus(),
                "携带有效 JWT 的请求不应被拒绝认证");
    }

    @Test
    @DisplayName("仅凭 X-User-Id 请求头 - 不得伪造认证（须校验 JWT 签名）")
    void headerOnlyRequestMustNotAuthenticate() throws Exception {
        clearPredefinedAuth();

        mockMvc.perform(get("/charger/page")
                        .header("X-User-Id", "1")
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("内部端点 - 缺少内部令牌应 401")
    void internalEndpointShouldRejectMissingToken() throws Exception {
        clearPredefinedAuth();

        mockMvc.perform(get("/internal/api/v1/chargers/by-code/NO-SUCH-CODE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("内部端点 - 携带正确内部令牌应放行并命中端点")
    void internalEndpointShouldAcceptInternalToken() throws Exception {
        clearPredefinedAuth();

        // 未知编码：端点命中并以统一 Result 包装返回（success=true, data=null）
        mockMvc.perform(get("/internal/api/v1/chargers/by-code/NO-SUCH-CODE")
                        .header("X-Internal-Token", "station-internal-test-token-0123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
