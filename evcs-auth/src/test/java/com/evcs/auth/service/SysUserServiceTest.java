package com.evcs.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.mapper.SysRoleMapper;
import com.evcs.auth.mapper.SysUserMapper;
import com.evcs.auth.mapper.SysUserRoleMapper;
import com.evcs.auth.service.impl.SysUserServiceImpl;
import com.evcs.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("系统用户服务测试")
class SysUserServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sysUserService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("获取用户信息 - 存在且租户匹配")
    void testGetUserByIdWithTenant_Success() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;
        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setUsername("testuser");

        // Mock selectOne with 2 args (wrapper, throwEx)
        // Based on previous error messages, this method is called.
        // We use lenient to avoid strict stubbing issues if implementation changes.
        lenient().doReturn(user).when(userMapper).selectOne(any(), anyBoolean());
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.singletonList(user));

        // Act
        SysUser result = sysUserService.getUserByIdWithTenant(userId, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(tenantId, result.getTenantId());
    }

    @Test
    @DisplayName("获取用户信息 - 不存在")
    void testGetUserByIdWithTenant_NotFound() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;

        lenient().doReturn(null).when(userMapper).selectOne(any(), anyBoolean());
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        SysUser result = sysUserService.getUserByIdWithTenant(userId, tenantId);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("重置密码 - 成功")
    void testResetPassword_Success() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;
        Long operatorId = 99L;
        String newEncodedPassword = "newEncodedPassword";

        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(tenantId);

        lenient().doReturn(user).when(userMapper).selectOne(any(), anyBoolean());
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.singletonList(user));
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // Act
        sysUserService.resetPassword(userId, newEncodedPassword, tenantId, operatorId);

        // Assert
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(userCaptor.capture());
        SysUser capturedUser = userCaptor.getValue();
        assertEquals(userId, capturedUser.getId());
        assertEquals(newEncodedPassword, capturedUser.getPassword());
        assertEquals(operatorId, capturedUser.getUpdateBy());
    }

    @Test
    @DisplayName("锁定账号 - 成功")
    void testLockAccount_Success() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;
        
        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setStatus(1); // 正常状态

        lenient().doReturn(user).when(userMapper).selectOne(any(), anyBoolean());
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.singletonList(user));
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // Act
        // 模拟Controller调用updateUser来锁定账号
        SysUser updateInfo = new SysUser();
        updateInfo.setId(userId);
        updateInfo.setStatus(0); // 禁用状态
        
        sysUserService.updateUser(updateInfo, tenantId);

        // Assert
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(userCaptor.capture());
        SysUser capturedUser = userCaptor.getValue();
        assertEquals(userId, capturedUser.getId());
        assertEquals(0, capturedUser.getStatus());
    }

    @Test
    @DisplayName("解锁账号 - 成功")
    void testUnlockAccount_Success() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 1001L;
        
        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setStatus(0); // 禁用状态

        lenient().doReturn(user).when(userMapper).selectOne(any(), anyBoolean());
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.singletonList(user));
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // Act
        SysUser updateInfo = new SysUser();
        updateInfo.setId(userId);
        updateInfo.setStatus(1); // 启用状态
        
        sysUserService.updateUser(updateInfo, tenantId);

        // Assert
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(userCaptor.capture());
        SysUser capturedUser = userCaptor.getValue();
        assertEquals(userId, capturedUser.getId());
        assertEquals(1, capturedUser.getStatus());
    }
}
