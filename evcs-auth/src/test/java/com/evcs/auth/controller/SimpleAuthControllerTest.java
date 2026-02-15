package com.evcs.auth.controller;

import com.evcs.auth.controller.dto.LoginResponse;
import com.evcs.auth.service.IAuthService;
import com.evcs.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("简化认证控制器测试")
class SimpleAuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @InjectMocks
    private SimpleAuthController controller;

    @Mock
    private IAuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("登录 - 有效请求应返回成功")
    void testLogin_shouldReturnSuccess_whenValidRequest() throws Exception {
        LoginResponse response = LoginResponse.builder()
            .accessToken("token-123")
            .tokenType("Bearer")
            .expiresIn(7200L)
            .user(Map.of("id", 1001L, "username", "alice"))
            .build();

        when(authService.login(eq("alice@example.com"), eq("password123"))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "identifier", "alice@example.com",
                    "password", "password123"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登录成功"))
            .andExpect(jsonPath("$.data.accessToken").value("token-123"));
    }

    @Test
    @DisplayName("用户信息 - 请求头携带用户与租户应直接透传")
    void testUserInfo_shouldReturnSuccess_whenHeadersProvided() throws Exception {
        when(authService.getUserInfo(anyLong(), anyLong())).thenReturn(Map.of(
            "id", 1001L,
            "tenantId", 2001L,
            "username", "alice"
        ));

        mockMvc.perform(get("/auth/userinfo")
                .header("X-User-Id", "1001")
                .header("X-Tenant-Id", "2001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("查询成功"))
            .andExpect(jsonPath("$.data.username").value("alice"));
    }
}