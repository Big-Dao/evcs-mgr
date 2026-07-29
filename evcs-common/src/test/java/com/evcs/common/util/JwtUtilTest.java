package com.evcs.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 启动校验测试。
 * 验证 jwt.secret 缺失/过短时启动失败（避免以弱密钥运行）。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    @DisplayName("缺失 jwt.secret 时 validateSecret 应抛出 IllegalStateException")
    void validateSecret_failsWhenSecretMissing() {
        // secret 保持 null（未注入）
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret")
        );
        assertTrue(ex.getMessage().contains("jwt.secret"));
    }

    @Test
    @DisplayName("空/空白 jwt.secret 时 validateSecret 应抛出 IllegalStateException")
    void validateSecret_failsWhenSecretBlank() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "   ");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret")
        );
        assertTrue(ex.getMessage().contains("jwt.secret"));
    }

    @Test
    @DisplayName("过短 jwt.secret（< 32 字符）时 validateSecret 应抛出 IllegalStateException")
    void validateSecret_failsWhenSecretTooShort() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "short-only-16chars"); // 16 字符

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret")
        );
        assertTrue(ex.getMessage().contains("过短"));
    }

    @Test
    @DisplayName("合法 jwt.secret（>= 32 字符）时 validateSecret 应通过")
    void validateSecret_passesWhenSecretValid() {
        // 40 字符的强密钥（>= 32 即合法）
        String strongSecret = "this-is-a-40-char-strong-secret-key!!!";
        assertTrue(strongSecret.length() >= 32);
        ReflectionTestUtils.setField(jwtUtil, "secret", strongSecret);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(jwtUtil, "validateSecret"));
    }

    @Test
    @DisplayName("合法密钥下 generateToken 与 verifyToken 应正常工作")
    void generateAndVerify_roundTripWithValidSecret() {
        String strongSecret = "this-is-a-40-char-strong-secret-key!!!";
        ReflectionTestUtils.setField(jwtUtil, "secret", strongSecret);
        // 注入 expire，避免 NPE
        ReflectionTestUtils.setField(jwtUtil, "expire", 3600L);

        String token = jwtUtil.generateToken(1L, "user", 100L);

        assertNotNull(token);
        assertTrue(jwtUtil.verifyToken(token));
        assertEquals(1L, jwtUtil.getUserId(token));
        assertEquals(100L, jwtUtil.getTenantId(token));
    }

    @Test
    @DisplayName("含角色的 generateToken 应在 getRoles 中还原角色列表")
    void generateTokenWithRoles_shouldRestoreRoles() {
        String strongSecret = "this-is-a-40-char-strong-secret-key!!!";
        ReflectionTestUtils.setField(jwtUtil, "secret", strongSecret);
        ReflectionTestUtils.setField(jwtUtil, "expire", 3600L);

        List<String> roles = List.of("ADMIN", "FINANCE");
        String token = jwtUtil.generateToken(1L, "user", 100L, roles);

        assertNotNull(token);
        assertEquals(roles, jwtUtil.getRoles(token));
    }

    @Test
    @DisplayName("getRoles 在无 roles claim 时应返回空列表")
    void getRoles_returnsEmptyWhenNoRolesClaim() {
        String strongSecret = "this-is-a-40-char-strong-secret-key!!!";
        ReflectionTestUtils.setField(jwtUtil, "secret", strongSecret);
        ReflectionTestUtils.setField(jwtUtil, "expire", 3600L);

        String token = jwtUtil.generateToken(1L, "user", 100L);

        assertNotNull(token);
        assertTrue(jwtUtil.getRoles(token).isEmpty());
    }
}
