package com.evcs.auth.filter;

import com.evcs.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TenantContextFilter 上下文绑定边界测试。
 *
 * <p>安全基线要求：tenant/user 身份只能来自网关转发的可信头（已从 JWT 派生）。
 * X-Tenant-Type / X-Tenant-Ancestors 不在网关剥离并重写的白名单内，不得从请求头绑定，
 * 否则登录用户可伪造平台管理员类型（tenantType=1）与任意祖先链。
 */
class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private record CapturedContext(Long tenantId, Long userId, Integer tenantType, String ancestors) {
    }

    private CapturedContext captureDuringRequest(HttpServletRequest request) throws ServletException, IOException {
        AtomicReference<Long> tenantId = new AtomicReference<>();
        AtomicReference<Long> userId = new AtomicReference<>();
        AtomicReference<Integer> tenantType = new AtomicReference<>();
        AtomicReference<String> ancestors = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            tenantId.set(TenantContext.getTenantId());
            userId.set(TenantContext.getUserId());
            tenantType.set(TenantContext.getTenantType());
            ancestors.set(TenantContext.getTenantAncestors());
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        return new CapturedContext(tenantId.get(), userId.get(), tenantType.get(), ancestors.get());
    }

    @Test
    @DisplayName("网关可信头 X-Tenant-Id/X-User-Id 应绑定进租户上下文")
    void shouldBindTenantIdAndUserIdFromGatewayHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");
        request.addHeader("X-User-Id", "7");

        CapturedContext captured = captureDuringRequest(request);

        assertEquals(100L, captured.tenantId());
        assertEquals(7L, captured.userId());
    }

    @Test
    @DisplayName("X-Tenant-Type 请求头不得绑定进租户上下文（防伪造平台管理员）")
    void shouldNotBindTenantTypeFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");
        request.addHeader("X-Tenant-Type", "1");

        CapturedContext captured = captureDuringRequest(request);

        assertNull(captured.tenantType(), "X-Tenant-Type 不可信，不得绑定");
    }

    @Test
    @DisplayName("X-Tenant-Ancestors 请求头不得绑定进租户上下文（防伪造祖先链越权）")
    void shouldNotBindTenantAncestorsFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");
        request.addHeader("X-Tenant-Ancestors", ",5,");

        CapturedContext captured = captureDuringRequest(request);

        assertNull(captured.ancestors(), "X-Tenant-Ancestors 不可信，不得绑定");
    }

    @Test
    @DisplayName("请求处理完成后应清理租户上下文")
    void shouldClearContextAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertNull(TenantContext.getTenantId(), "请求结束后租户上下文应被清理");
    }
}
