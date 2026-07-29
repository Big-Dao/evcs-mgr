package com.evcs.payment.security;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 测试（payment 服务侧）。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("有效 JWT 时应认证成功，角色加 ROLE_ 前缀")
    void shouldAuthenticateWithRolePrefixWhenJwtValid() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);
        when(jwtUtil.getRoles(token)).thenReturn(java.util.List.of("ADMIN", "FINANCE"));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FINANCE")));
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("角色已含 ROLE_ 前缀时不应重复加前缀")
    void shouldNotDuplicateRolePrefix() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);
        when(jwtUtil.getRoles(token)).thenReturn(java.util.List.of("ROLE_ADMIN"));

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        long adminCount = auth.getAuthorities().stream()
                .filter(a -> a.getAuthority().equals("ROLE_ADMIN"))
                .count();
        assertEquals(1, adminCount, "ROLE_ADMIN 应只出现一次，不应重复加前缀");
    }

    @Test
    @DisplayName("无 Authorization 头时应放行但不认证")
    void shouldPassWithoutAuthButNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

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
    void shouldPassSafelyWhenProcessingThrows() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.verifyToken(token)).thenReturn(true);
        when(jwtUtil.getUserId(token)).thenReturn(1L);
        when(jwtUtil.getTenantId(token)).thenReturn(100L);
        when(jwtUtil.getRoles(token)).thenThrow(new RuntimeException("decode error"));

        assertDoesNotThrow(() ->
                filter.doFilter(request, new MockHttpServletResponse(), filterChain));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }
}
