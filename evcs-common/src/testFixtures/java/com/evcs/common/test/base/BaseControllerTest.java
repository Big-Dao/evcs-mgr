package com.evcs.common.test.base;

import com.evcs.common.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Controller层测试基类
 * 提供MockMvc、ObjectMapper和多租户上下文支持
 * 
 * 使用方法：
 * <pre>
 * @SpringBootTest
 * class YourControllerTest extends BaseControllerTest {
 *     
 *     @Test
 *     void testGetApi() throws Exception {
 *         mockMvc.perform(get("/api/resource")
 *                 .header("Authorization", "Bearer " + getTestToken()))
 *                 .andExpect(status().isOk())
 *                 .andExpect(jsonPath("$.code").value(200));
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
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

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
        var authorities = AuthorityUtils.createAuthorityList("ROLE_TEST");
        var authentication = new UsernamePasswordAuthenticationToken("test-user", "N/A", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 测试后清理租户上下文
     */
    @AfterEach
    public void tearDownTenantContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /**
     * 获取测试租户ID
     * 
     * @return 测试租户ID
     */
    protected Long getTestTenantId() {
        return DEFAULT_TENANT_ID;
    }

    /**
     * 获取测试用户ID
     * 
     * @return 测试用户ID
     */
    protected Long getTestUserId() {
        return DEFAULT_USER_ID;
    }

    /**
     * 将对象转换为JSON字符串
     * 
     * @param object 要转换的对象
     * @return JSON字符串
     */
    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * 从JSON字符串解析对象
     * 
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 解析后的对象
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

    /**
     * 从MvcResult中提取响应体并解析为对象
     * 
     * @param result MvcResult
     * @param clazz 目标类型
     * @return 解析后的对象
     */
    protected <T> T extractResponse(MvcResult result, Class<T> clazz) throws Exception {
        String content = result.getResponse().getContentAsString();
        return fromJson(content, clazz);
    }

    /**
     * 获取测试JWT Token（子类可以覆盖）
     * 
     * @return JWT Token
     */
    protected String getTestToken() {
        // 默认返回空，子类可以覆盖提供真实的测试token
        return "test-token";
    }

    /**
     * 打印响应内容（用于调试）
     * 
     * @param result MvcResult
     */
    protected void printResponse(MvcResult result) throws Exception {
        print().handle(result);
    }
}
