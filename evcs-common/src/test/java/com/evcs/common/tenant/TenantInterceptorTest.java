package com.evcs.common.tenant;

import com.evcs.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TenantInterceptor 专项测试。
 * 验证租户解析的三条路径，以及 ?tenantId= 参数回退已被移除（防租户注入）。
 */
@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    private TenantInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor(jwtUtil);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("路径1：优先从 X-Tenant-Id/X-User-Id 头解析租户")
    void resolvesFromHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");
        request.addHeader("X-User-Id", "200");

        boolean ok = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(ok);
        assertEquals(100L, TenantContext.getTenantId());
        assertEquals(200L, TenantContext.getUserId());
        verify(jwtUtil, never()).verifyToken(anyString());
    }

    @Test
    @DisplayName("路径2：无头时从 JWT Token 提取租户")
    void resolvesFromJwtWhenNoHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtUtil.verifyToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.getTenantId("valid.jwt.token")).thenReturn(42L);
        when(jwtUtil.getUserId("valid.jwt.token")).thenReturn(7L);

        boolean ok = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(ok);
        assertEquals(42L, TenantContext.getTenantId());
        assertEquals(7L, TenantContext.getUserId());
    }

    @Test
    @DisplayName("路径3：头优先于 JWT (头存在时不解析 JWT)")
    void headersTakePrecedenceOverJwt() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertEquals(100L, TenantContext.getTenantId(), "Header 应优先于 JWT");
        verify(jwtUtil, never()).verifyToken(anyString());
    }

    @Test
    @DisplayName("安全：?tenantId= 请求参数不再被信任 (防租户注入旁路)")
    void tenantIdParameterIsNotTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("tenantId", "999");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertNull(TenantContext.getTenantId(),
                "?tenantId= 参数不应再被解析为租户身份");
    }

    @Test
    @DisplayName("安全：?tenantId= 参数无法覆盖已存在的可信头")
    void tenantIdParameterCannotOverrideHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");
        request.addParameter("tenantId", "999");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertEquals(100L, TenantContext.getTenantId(),
                "可信头应优先，参数回退已删除");
    }

    @Test
    @DisplayName("边界：无效的 X-Tenant-Id 头应回退到 JWT 路径")
    void invalidHeaderFallsBackToJwt() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "not-a-number");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        when(jwtUtil.verifyToken("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.getTenantId("valid.jwt.token")).thenReturn(55L);
        when(jwtUtil.getUserId("valid.jwt.token")).thenReturn(6L);

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertEquals(55L, TenantContext.getTenantId(), "无效头应回退到 JWT");
    }

    @Test
    @DisplayName("边界：JWT 验证失败时不设置租户上下文")
    void invalidJwtDoesNotSetTenant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.jwt.token");

        when(jwtUtil.verifyToken("bad.jwt.token")).thenReturn(false);

        boolean ok = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(ok, "即使无租户上下文，拦截器仍应放行（兼容公开接口）");
        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("边界：无任何租户信息时继续放行 (fail-open，兼容公开接口)")
    void noTenantInfoStillProceeds() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        boolean ok = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(ok);
        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("清理：afterCompletion 会清除租户上下文")
    void afterCompletionClearsContext() {
        TenantContext.setTenantId(100L);

        interceptor.afterCompletion(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), null);

        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("resolveTenantId 值对象：正确反映来源")
    void resolveTenantIdValueObject() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "100");

        TenantInterceptor.ResolvedTenant result = interceptor.resolveTenantId(request);

        assertEquals(100L, result.tenantId);
        assertEquals(TenantInterceptor.ResolvedTenant.Source.HEADER, result.source);
    }
}
