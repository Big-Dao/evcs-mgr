package com.evcs.common.test.base;

import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service层测试基类
 * 提供多租户上下文管理和事务回滚支持
 *
 * 使用方法：
 * <pre>
 * @SpringBootTest(classes = YourApplication.class)
 * class YourServiceTest extends BaseServiceTest {
 *     @Resource
 *     private YourService yourService;
 *
 *     @Test
 *     void testYourMethod() {
 *         // 测试代码，租户上下文已自动设置
 *     }
 * }
 * </pre>
 */
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.datasource.hikari.minimum-idle=1"
})
@Transactional
@Rollback
public abstract class BaseServiceTest {

    /**
     * 默认测试租户ID
     */
    protected static final Long DEFAULT_TENANT_ID = 1L;

    /**
     * 默认测试用户ID
     */
    protected static final Long DEFAULT_USER_ID = 1L;

    /**
     * 测试前设置租户上下文
     */
    @BeforeEach
    public void setUpTenantContext() {
        TenantContext.setCurrentTenantId(getTestTenantId());
        TenantContext.setCurrentUserId(getTestUserId());
    }

    /**
     * 测试后清理租户上下文
     */
    @AfterEach
    public void tearDownTenantContext() {
        TenantContext.clear();
    }

    /**
     * 获取测试租户ID
     * 子类可以覆盖此方法以使用不同的租户ID
     *
     * @return 测试租户ID
     */
    protected Long getTestTenantId() {
        return DEFAULT_TENANT_ID;
    }

    /**
     * 获取测试用户ID
     * 子类可以覆盖此方法以使用不同的用户ID
     *
     * @return 测试用户ID
     */
    protected Long getTestUserId() {
        return DEFAULT_USER_ID;
    }

    /**
     * 切换租户上下文
     * 用于测试多租户隔离场景
     *
     * @param tenantId 新的租户ID
     */
    protected void switchTenant(Long tenantId) {
        TenantContext.setCurrentTenantId(tenantId);
    }

    /**
     * 切换用户上下文
     *
     * @param userId 新的用户ID
     */
    protected void switchUser(Long userId) {
        TenantContext.setCurrentUserId(userId);
    }
}
