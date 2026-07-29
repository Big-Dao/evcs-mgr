package com.evcs.auth.security;

import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.ISysUserService;
import com.evcs.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 测试。
 * 验证 JWT 校验、用户加载、角色注入、异常路径。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ISysUserService userService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuthJwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("有效 JWT + 活跃用户时应认证成功并注入角色")
    void shouldAuthenticateWhenJwtValidAndUserActive() throws Exception {
        String token = "valid.jwt.token";
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(100L);
        user.setUsername("alice");
        user.setStatus(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);
        when(userService.getUserByIdWithTenant(1L, 100L)).thenReturn(user);
        when(userService.listRoleCodes(1L)).thenReturn(List.of("ROLE_ADMIN", "ROLE_USER"));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "应已认证");
        assertEquals(user, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("无 Authorization 头时应放行但不认证")
    void shouldPassWithoutAuthButNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
        verify(jwtUtil, never()).verifyToken(any());
    }

    @Test
    @DisplayName("非 Bearer 格式的 Authorization 头应放行但不认证")
    void shouldPassWithoutBearerButNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("JWT 校验失败时应放行但不认证")
    void shouldPassWhenJwtInvalid() throws Exception {
        String token = "bad.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("JWT 对应用户已禁用时应不认证")
    void shouldNotAuthenticateWhenUserDisabled() throws Exception {
        String token = "valid.jwt.token";
        SysUser user = new SysUser();
        user.setId(1L);
        user.setTenantId(100L);
        user.setStatus(0); // 禁用

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);
        when(userService.getUserByIdWithTenant(1L, 100L)).thenReturn(user);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "禁用用户不应认证");
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("JWT 缺少 userId/tenantId 时应放行但不认证")
    void shouldPassWhenJwtMissingClaims() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(null);
        when(jwtUtil.getTenantId(token)).thenReturn(null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("用户加载异常时应安全放行（不抛出）")
    void shouldPassSafelyWhenUserLoadThrows() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);
        when(userService.getUserByIdWithTenant(1L, 100L))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() ->
                filter.doFilter(request, new MockHttpServletResponse(), filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }
}
