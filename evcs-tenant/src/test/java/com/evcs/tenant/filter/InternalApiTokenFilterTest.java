package com.evcs.tenant.filter;

import com.evcs.tenant.config.InternalApiTokenProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class InternalApiTokenFilterTest {

    @Test
    @DisplayName("内部接口鉴权 - 缺少 token 时应拒绝")
    void testInternalApi_shouldDeny_whenTokenMissing() throws Exception {
        // Arrange
        InternalApiTokenProperties properties = new InternalApiTokenProperties();
        properties.setEnabled(true);
        properties.setToken("t");

        InternalApiTokenFilter filter = new InternalApiTokenFilter(properties);
        filter.validateConfig();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/api/v1/tenant-hierarchy/descendant");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertFalse(proceeded.get(), "鉴权失败时不应继续执行后续链路");
        assertEquals(401, response.getStatus(), "缺少 token 应返回 401");
        assertTrue(response.getContentAsString().contains("内部接口鉴权失败"), "响应体应包含失败原因");
    }

    @Test
    @DisplayName("内部接口鉴权 - token 正确时应放行")
    void testInternalApi_shouldAllow_whenTokenValid() throws Exception {
        // Arrange
        InternalApiTokenProperties properties = new InternalApiTokenProperties();
        properties.setEnabled(true);
        properties.setToken("t");

        InternalApiTokenFilter filter = new InternalApiTokenFilter(properties);
        filter.validateConfig();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/api/v1/tenant-hierarchy/descendant");
        request.addHeader("X-Internal-Token", "t");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertTrue(proceeded.get(), "token 正确时应继续执行后续链路");
        assertEquals(200, response.getStatus(), "Mock response 默认应保持 200");
    }

    @Test
    @DisplayName("内部接口鉴权 - 非内部路径不应拦截")
    void testNonInternal_shouldNotFilter() throws Exception {
        // Arrange
        InternalApiTokenProperties properties = new InternalApiTokenProperties();
        properties.setEnabled(true);
        properties.setToken("t");

        InternalApiTokenFilter filter = new InternalApiTokenFilter(properties);
        filter.validateConfig();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean proceeded = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> proceeded.set(true);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertTrue(proceeded.get(), "非内部路径不应被拦截");
    }
}
