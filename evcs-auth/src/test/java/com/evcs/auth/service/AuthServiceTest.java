package com.evcs.auth.service;

import com.evcs.auth.controller.dto.LoginResponse;
import com.evcs.auth.controller.dto.UserCreateRequest;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.impl.AuthServiceImpl;
import com.evcs.common.exception.BusinessException;
import com.evcs.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpireSeconds", 7200L);
    }

    @Test
    @DisplayName("用户登录 - 有效凭证")
    void testLogin_WithValidCredentials() {
        // Arrange
        String identifier = "testuser";
        String password = "password123";
        String encodedPassword = "encodedPassword";
        Long userId = 1L;
        Long tenantId = 1001L;

        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(identifier);
        user.setPassword(encodedPassword);
        user.setTenantId(tenantId);
        user.setStatus(1);
        user.setLoginIdentifier(identifier);

        when(userService.getByIdentifier(identifier)).thenReturn(user);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(jwtUtil.generateToken(userId, identifier, tenantId)).thenReturn("mock-token");
        when(jwtUtil.getExpiration("mock-token")).thenReturn(Instant.now().plusSeconds(7200));
        when(userService.listRoleCodes(userId)).thenReturn(Collections.singletonList("ADMIN"));

        // Act
        LoginResponse response = authService.login(identifier, password);

        // Assert
        assertNotNull(response);
        assertEquals("mock-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals(identifier, response.getUser().get("username"));
        
        verify(userService).getByIdentifier(identifier);
        verify(passwordEncoder).matches(password, encodedPassword);
        verify(jwtUtil).generateToken(userId, identifier, tenantId);
    }

    @Test
    @DisplayName("用户登录 - 无效凭证")
    void testLogin_WithInvalidCredentials() {
        // Arrange
        String identifier = "testuser";
        String password = "wrongpassword";
        String encodedPassword = "encodedPassword";

        SysUser user = new SysUser();
        user.setPassword(encodedPassword);
        user.setTenantId(1L);
        user.setStatus(1);

        when(userService.getByIdentifier(identifier)).thenReturn(user);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.login(identifier, password));
    }

    @Test
    @DisplayName("刷新Token - 有效Token")
    void testRefreshToken_WithValidToken() {
        // Arrange
        String oldToken = "old-token";
        String newToken = "new-token";

        when(jwtUtil.verifyToken(oldToken)).thenReturn(true);
        when(jwtUtil.refreshToken(oldToken)).thenReturn(newToken);

        // Act
        LoginResponse response = authService.refreshToken(oldToken);

        // Assert
        assertNotNull(response);
        assertEquals(newToken, response.getAccessToken());
    }

    @Test
    @DisplayName("验证Token - 有效Token")
    void testValidateToken_WithValidToken() {
        // Arrange
        String token = "valid-token";
        when(jwtUtil.verifyToken(token)).thenReturn(true);

        // Act
        boolean isValid = authService.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("验证Token - 过期Token")
    void testValidateToken_WithExpiredToken() {
        // Arrange
        String token = "expired-token";
        when(jwtUtil.verifyToken(token)).thenReturn(false);

        // Act
        boolean isValid = authService.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("用户注册 - 正常流程")
    void testRegister_WithValidData() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest();
        request.setLoginIdentifier("newuser");
        request.setPassword("password123");
        request.setUsername("New User");

        when(userService.getByIdentifier(request.getLoginIdentifier())).thenReturn(null);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        
        // Act
        authService.register(request);

        // Assert
        verify(userService).createUser(any(SysUser.class), any());
    }

    @Test
    @DisplayName("用户注册 - 重复用户名")
    void testRegister_WithDuplicateUsername() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest();
        request.setLoginIdentifier("existinguser");

        when(userService.getByIdentifier(request.getLoginIdentifier())).thenReturn(new SysUser());

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("修改密码 - 正常流程")
    void testChangePassword() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "oldPass";
        String newPassword = "newPass";
        String encodedOldPass = "encodedOld";

        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(encodedOldPass);
        user.setTenantId(1001L);

        when(userService.getUserByIdWithTenant(eq(userId), any())).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, encodedOldPass)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNew");

        // Act
        authService.changePassword(userId, oldPassword, newPassword);

        // Assert
        verify(userService).resetPassword(eq(userId), eq("encodedNew"), eq(1001L), eq(userId));
    }

    @Test
    @DisplayName("刷新Token - 无效Token")
    void testRefreshToken_WithInvalidToken() {
        // Arrange
        String invalidToken = "invalid-token";
        when(jwtUtil.verifyToken(invalidToken)).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.refreshToken(invalidToken));
    }

    @Test
    @DisplayName("获取用户信息 - 有效用户应返回用户信息")
    void testGetUserInfo_shouldReturnUserInfo_whenUserExists() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;

        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setUsername("testuser");
        user.setLoginIdentifier("testuser");
        user.setStatus(1);

        when(userService.getUserByIdWithTenant(userId, tenantId)).thenReturn(user);
        when(userService.listRoleCodes(userId)).thenReturn(Collections.singletonList("ADMIN"));

        // Act
        Map<String, Object> userInfo = authService.getUserInfo(userId, tenantId);

        // Assert
        assertNotNull(userInfo);
        assertEquals(userId, userInfo.get("id"));
        assertEquals(tenantId, userInfo.get("tenantId"));
        assertEquals("testuser", userInfo.get("username"));
        assertEquals("testuser", userInfo.get("identifier"));
        assertEquals(Collections.singletonList("ADMIN"), userInfo.get("roles"));

        verify(userService).getUserByIdWithTenant(userId, tenantId);
        verify(userService).listRoleCodes(userId);
    }

    @Test
    @DisplayName("获取用户信息 - 缺少上下文应抛出未登录异常")
    void testGetUserInfo_shouldThrowBusinessException_whenMissingContext() {
        // Arrange
        Long userId = null;
        Long tenantId = 1001L;

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.getUserInfo(userId, tenantId));
    }

    @Test
    @DisplayName("获取用户信息 - 用户不存在应抛出异常")
    void testGetUserInfo_shouldThrowBusinessException_whenUserNotFound() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;

        when(userService.getUserByIdWithTenant(userId, tenantId)).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.getUserInfo(userId, tenantId));
        verify(userService).getUserByIdWithTenant(userId, tenantId);
        verify(userService, never()).listRoleCodes(anyLong());
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误")
    void testChangePassword_WithWrongOldPassword() {
        // Arrange
        Long userId = 1L;
        String oldPassword = "wrongOldPassword";
        String newPassword = "newPassword";
        String encodedOldPass = "encodedOld";

        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(encodedOldPass);
        user.setTenantId(1001L);

        when(userService.getUserByIdWithTenant(eq(userId), any())).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, encodedOldPass)).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authService.changePassword(userId, oldPassword, newPassword));
    }
}
