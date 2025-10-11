package com.evcs.common.tenant;

import com.evcs.common.exception.TenantContextMissingException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomTenantLineHandler Security Tests
 * Tests for Week 1 - Day 2-3: Security Vulnerability Fixes (SEC-02)
 * 
 * Validates that the tenant line handler properly throws exceptions
 * when tenant context is missing, preventing potential data leaks.
 */
class CustomTenantLineHandlerTest {

    private CustomTenantLineHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CustomTenantLineHandler();
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void testGetTenantIdWithValidContext() {
        // Setup tenant context
        TenantContext.setTenantId(123L);

        // Get tenant ID from handler
        Expression expression = handler.getTenantId();

        assertNotNull(expression);
        assertTrue(expression instanceof LongValue);
        assertEquals(123L, ((LongValue) expression).getValue());
    }

    @Test
    void testGetTenantIdThrowsExceptionWhenContextMissing() {
        // Ensure context is clear
        TenantContext.clear();

        // Should throw exception when tenant context is missing
        TenantContextMissingException exception = assertThrows(
            TenantContextMissingException.class,
            () -> handler.getTenantId(),
            "Should throw TenantContextMissingException when tenant context is missing"
        );

        assertTrue(exception.getMessage().contains("租户上下文缺失"));
    }

    @Test
    void testGetTenantIdColumn() {
        assertEquals("tenant_id", handler.getTenantIdColumn());
    }

    @Test
    void testIgnoreSystemTables() {
        // System management tables should be ignored
        assertTrue(handler.ignoreTable("sys_tenant"));
        assertTrue(handler.ignoreTable("sys_dict_type"));
        assertTrue(handler.ignoreTable("sys_dict_data"));
        assertTrue(handler.ignoreTable("sys_config"));
        assertTrue(handler.ignoreTable("sys_log"));
    }

    @Test
    void testIgnoreBaseTables() {
        // Base data tables should be ignored
        assertTrue(handler.ignoreTable("base_region"));
        assertTrue(handler.ignoreTable("base_bank"));
    }

    @Test
    void testIgnoreStatTables() {
        // Statistics tables should be ignored
        assertTrue(handler.ignoreTable("stat_global"));
    }

    @Test
    void testIgnoreTempAndCacheTables() {
        // Tables with temp_ or cache_ prefix should be ignored
        assertTrue(handler.ignoreTable("temp_data"));
        assertTrue(handler.ignoreTable("temp_users"));
        assertTrue(handler.ignoreTable("cache_sessions"));
        assertTrue(handler.ignoreTable("cache_tokens"));
    }

    @Test
    void testDoNotIgnoreBusinessTables() {
        // Business tables should NOT be ignored
        assertFalse(handler.ignoreTable("station"));
        assertFalse(handler.ignoreTable("charger"));
        assertFalse(handler.ignoreTable("charging_order"));
        assertFalse(handler.ignoreTable("payment_record"));
        assertFalse(handler.ignoreTable("tenant"));
    }

    @Test
    void testSystemAdminStillRequiresTenantIsolation() {
        // Even for system admin, tables should not be automatically ignored
        TenantContext.setTenantType(1); // System admin
        TenantContext.setTenantId(1L);

        // Business tables should still require tenant filtering
        assertFalse(handler.ignoreTable("station"));
        assertFalse(handler.ignoreTable("charger"));
    }

    @Test
    void testMultipleTenantsInSequence() {
        // Test switching between tenants
        TenantContext.setTenantId(100L);
        Expression expr1 = handler.getTenantId();
        assertEquals(100L, ((LongValue) expr1).getValue());

        TenantContext.setTenantId(200L);
        Expression expr2 = handler.getTenantId();
        assertEquals(200L, ((LongValue) expr2).getValue());

        TenantContext.setTenantId(300L);
        Expression expr3 = handler.getTenantId();
        assertEquals(300L, ((LongValue) expr3).getValue());
    }

    @Test
    void testTenantIdZeroIsValid() {
        // Tenant ID 0 should be valid (e.g., for root tenant)
        TenantContext.setTenantId(0L);
        
        Expression expression = handler.getTenantId();
        assertNotNull(expression);
        assertEquals(0L, ((LongValue) expression).getValue());
    }
}
