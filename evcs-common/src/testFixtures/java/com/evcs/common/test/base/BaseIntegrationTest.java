package com.evcs.common.test.base;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集成测试基类
 * 用于需要完整Spring上下文的集成测试
 * 
 * 集成测试特点：
 * - 启动完整的Spring Boot应用上下文
 * - 可以测试多个组件之间的集成
 * - 运行时间较长，适合验证端到端流程
 * 
 * 使用方法：
 * <pre>
 * @SpringBootTest(classes = YourApplication.class, 
 *                 webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * class YourIntegrationTest extends BaseIntegrationTest {
 *     
 *     @Autowired
 *     private TestRestTemplate restTemplate;
 *     
 *     @Test
 *     void testEndToEndFlow() {
 *         // 测试完整的业务流程
 *     }
 * }
 * </pre>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Tag("integration")
public abstract class BaseIntegrationTest extends BaseServiceTest {

    /**
     * 等待异步操作完成
     * 
     * @param millis 等待毫秒数
     */
    protected void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待被中断", e);
        }
    }

    /**
     * 重试操作直到成功或超时
     * 
     * @param action 要执行的操作
     * @param maxAttempts 最大重试次数
     * @param delayMillis 重试间隔（毫秒）
     * @return 是否成功
     */
    protected boolean retryUntilSuccess(Runnable action, int maxAttempts, long delayMillis) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                action.run();
                return true;
            } catch (Exception e) {
                if (i < maxAttempts - 1) {
                    waitFor(delayMillis);
                }
            }
        }
        return false;
    }
}
