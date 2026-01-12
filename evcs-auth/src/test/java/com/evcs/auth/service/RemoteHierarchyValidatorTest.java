package com.evcs.auth.service;

import com.evcs.auth.client.TenantHierarchyClient;
import com.evcs.auth.service.impl.RemoteHierarchyValidator;
import com.evcs.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("租户层级校验器 - 远程实现")
class RemoteHierarchyValidatorTest {

    @Mock
    private TenantHierarchyClient tenantHierarchyClient;

    @InjectMocks
    private RemoteHierarchyValidator validator;

    @Test
    @DisplayName("isDescendant - 远程返回true应放行")
    void testIsDescendant_shouldReturnTrue_whenRemoteReturnsTrue() {
        // Arrange
        Long currentTenantId = 100L;
        Long targetTenantId = 200L;
        when(tenantHierarchyClient.isDescendant(currentTenantId, targetTenantId)).thenReturn(Result.success(true));

        // Act
        boolean result = validator.isDescendant(currentTenantId, targetTenantId);

        // Assert
        assertTrue(result);
        verify(tenantHierarchyClient).isDescendant(currentTenantId, targetTenantId);
    }

    @Test
    @DisplayName("isDescendant - 远程失败应fail-closed")
    void testIsDescendant_shouldReturnFalse_whenRemoteReturnsFailure() {
        // Arrange
        Long currentTenantId = 100L;
        Long targetTenantId = 200L;
        when(tenantHierarchyClient.isDescendant(currentTenantId, targetTenantId)).thenReturn(Result.failure("down"));

        // Act
        boolean result = validator.isDescendant(currentTenantId, targetTenantId);

        // Assert
        assertFalse(result);
        verify(tenantHierarchyClient).isDescendant(currentTenantId, targetTenantId);
    }

    @Test
    @DisplayName("isDescendant - 远程异常应fail-closed")
    void testIsDescendant_shouldReturnFalse_whenRemoteThrows() {
        // Arrange
        Long currentTenantId = 100L;
        Long targetTenantId = 200L;
        when(tenantHierarchyClient.isDescendant(currentTenantId, targetTenantId))
                .thenThrow(new RuntimeException("timeout"));

        // Act
        boolean result = validator.isDescendant(currentTenantId, targetTenantId);

        // Assert
        assertFalse(result);
        verify(tenantHierarchyClient).isDescendant(currentTenantId, targetTenantId);
    }
}
