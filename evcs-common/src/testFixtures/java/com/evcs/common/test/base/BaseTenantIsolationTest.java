package com.evcs.common.test.base;

import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多租户隔离测试基类
 * 提供租户隔离测试的通用方法和断言
 * 
 * 使用方法：
 * <pre>
 * @SpringBootTest(classes = YourApplication.class)
 * class YourTenantIsolationTest extends BaseTenantIsolationTest {
 *     
 *     @Test
 *     void testDataIsolation() {
 *         // 租户1创建数据
 *         Long dataId = runAsTenant(1L, () -> {
 *             // 创建数据的代码
 *             return createdDataId;
 *         });
 *         
 *         // 租户2不能访问租户1的数据
 *         runAsTenant(2L, () -> {
 *             Data data = dataService.getById(dataId);
 *             assertNull(data, "租户2不应该能访问租户1的数据");
 *         });
 *     }
 * }
 * </pre>
 */
@ActiveProfiles("test")
@Transactional
public abstract class BaseTenantIsolationTest {

    /**
     * 测试后清理租户上下文
     */
    @AfterEach
    public void tearDownTenantContext() {
        TenantContext.clear();
    }

    /**
     * 以指定租户身份执行操作
     * 
     * @param tenantId 租户ID
     * @param action 要执行的操作
     */
    protected void runAsTenant(Long tenantId, Runnable action) {
        try {
            TenantContext.setCurrentTenantId(tenantId);
            action.run();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 以指定租户身份执行操作并返回结果
     * 
     * @param tenantId 租户ID
     * @param supplier 要执行的操作
     * @return 操作结果
     * @throws Exception 如果操作执行时抛出异常
     */
    protected <T> T runAsTenant(Long tenantId, TenantSupplier<T> supplier) throws Exception {
        try {
            TenantContext.setCurrentTenantId(tenantId);
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 以指定租户和用户身份执行操作
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param action 要执行的操作
     */
    protected void runAsTenantUser(Long tenantId, Long userId, Runnable action) {
        try {
            TenantContext.setCurrentTenantId(tenantId);
            TenantContext.setCurrentUserId(userId);
            action.run();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 断言租户上下文已设置
     */
    protected void assertTenantContextSet() {
        assertNotNull(TenantContext.getCurrentTenantId(), "租户上下文应该已设置");
    }

    /**
     * 断言租户上下文未设置
     */
    protected void assertTenantContextNotSet() {
        assertNull(TenantContext.getCurrentTenantId(), "租户上下文应该未设置");
    }

    /**
     * 断言当前租户ID
     * 
     * @param expectedTenantId 期望的租户ID
     */
    protected void assertCurrentTenant(Long expectedTenantId) {
        assertEquals(expectedTenantId, TenantContext.getCurrentTenantId(), 
                    "当前租户ID应该是 " + expectedTenantId);
    }

    /**
     * 断言数据属于指定租户
     * 
     * @param expectedTenantId 期望的租户ID
     * @param actualTenantId 实际的租户ID
     */
    protected void assertDataBelongsToTenant(Long expectedTenantId, Long actualTenantId) {
        assertEquals(expectedTenantId, actualTenantId, 
                    "数据应该属于租户 " + expectedTenantId);
    }

    /**
     * 断言租户1不能访问租户2的数据
     * 
     * @param data 数据对象
     * @param message 错误消息
     */
    protected void assertTenantIsolation(Object data, String message) {
        assertNull(data, message != null ? message : "应该无法访问其他租户的数据");
    }

    /**
     * Supplier函数式接口，支持抛出异常
     */
    @FunctionalInterface
    protected interface TenantSupplier<T> {
        T get() throws Exception;
    }
}
