package com.evcs.order.service;

import static org.junit.jupiter.api.Assertions.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.mapper.ChargingOrderMapper;
import com.evcs.order.service.IBillingPlanCacheService;
import com.evcs.order.service.IBillingService;
import com.evcs.order.service.IChargingOrderService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 订单服务测试模板
 *
 * 使用说明:
 * 1. 取消注释@SpringBootTest注解并指定正确的Application类
 * 2. 注入需要测试的Service
 * 3. 根据实际业务编写测试用例
 */
@SpringBootTest(
    classes = { com.evcs.order.OrderServiceApplication.class },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@DisplayName("订单服务测试")
class OrderServiceTest extends BaseServiceTest {

    @Resource
    private IChargingOrderService orderService;

    @MockBean
    private IBillingService billingService;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @SpyBean
    private ChargingOrderMapper chargingOrderMapper;

    @MockBean
    private IBillingPlanCacheService billingPlanCacheService;

    @org.junit.jupiter.api.BeforeEach
    void initMocks() {
        // Stub MeterRegistry.counter to return a mock Counter to avoid NPEs during tests
        io.micrometer.core.instrument.Counter mockCounter =
            org.mockito.Mockito.mock(
                io.micrometer.core.instrument.Counter.class
            );
        org.mockito.Mockito.when(
            meterRegistry.counter(org.mockito.ArgumentMatchers.anyString())
        ).thenReturn(mockCounter);

        // Stub billingService.calculateAmount to always return zero amount to simplify tests
        java.math.BigDecimal zero = java.math.BigDecimal.ZERO;
        org.mockito.Mockito.when(
            billingService.calculateAmount(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong()
            )
        ).thenReturn(zero);

        // Stub mapper.updateById to avoid Missing Mapped Statement issues in test environment
        org.mockito.Mockito.doReturn(1)
            .when(chargingOrderMapper)
            .updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("创建订单 - 正常流程")
    void testCreateOrder() {
        Long stationId = TestDataFactory.generateId();
        Long chargerId = TestDataFactory.generateId();
        String sessionId = TestDataFactory.generateCode("SESSION");
        Long userId = TestDataFactory.generateId();

        boolean result = orderService.createOrderOnStart(
            stationId,
            chargerId,
            sessionId,
            userId,
            null
        );
        assertTrue(result);

        ChargingOrder order = orderService.getBySessionId(sessionId);
        assertNotNull(order);
        assertEquals(DEFAULT_TENANT_ID, order.getTenantId());
        assertEquals(stationId, order.getStationId());
        assertEquals(chargerId, order.getChargerId());
    }

    @Test
    @DisplayName("开始充电 - 正常流程")
    void testStartCharging() {
        Long stationId = TestDataFactory.generateId();
        Long chargerId = TestDataFactory.generateId();
        String sessionId = TestDataFactory.generateCode("SESSION");
        Long userId = TestDataFactory.generateId();

        assertTrue(
            orderService.createOrderOnStart(
                stationId,
                chargerId,
                sessionId,
                userId,
                null
            )
        );
        // idempotent: second start should be allowed and return true
        assertTrue(
            orderService.createOrderOnStart(
                stationId,
                chargerId,
                sessionId,
                userId,
                null
            )
        );

        ChargingOrder order = orderService.getBySessionId(sessionId);
        assertNotNull(order);
        assertEquals(ChargingOrder.STATUS_CREATED, order.getStatus());
    }

    @Test
    @DisplayName("停止充电 - 正常流程")
    void testStopCharging() {
        String sessionId = TestDataFactory.generateCode("SESSION");
        Long stationId = TestDataFactory.generateId();
        Long chargerId = TestDataFactory.generateId();
        Long userId = TestDataFactory.generateId();

        assertTrue(
            orderService.createOrderOnStart(
                stationId,
                chargerId,
                sessionId,
                userId,
                null
            )
        );
        boolean completed = orderService.completeOrderOnStop(
            sessionId,
            12.5,
            30L
        );
        assertTrue(completed);

        ChargingOrder order = orderService.getBySessionId(sessionId);
        assertNotNull(order);
        assertEquals(ChargingOrder.STATUS_COMPLETED, order.getStatus());
        assertEquals(Double.valueOf(12.5), order.getEnergy());
        assertEquals(Long.valueOf(30L), order.getDuration());
        assertNotNull(order.getEndTime());
    }

    @Test
    @DisplayName("查询订单列表 - 分页查询")
    void testQueryOrderPage() {
        // create orders
        for (int i = 0; i < 5; i++) {
            orderService.createOrderOnStart(
                TestDataFactory.generateId(),
                TestDataFactory.generateId(),
                TestDataFactory.generateCode("SESSION"),
                TestDataFactory.generateId(),
                null
            );
        }

        Page<ChargingOrder> page = new Page<>(1, 10);
        IPage<ChargingOrder> result = orderService.pageOrders(
            page,
            null,
            null,
            null,
            null
        );
        assertNotNull(result);
        assertTrue(result.getRecords().size() >= 5);
    }

    @Test
    @DisplayName("订单幂等性 - 重复创建应该返回原订单")
    void testOrderIdempotency() {
        String sessionId = TestDataFactory.generateCode("SESSION");
        Long stationId = TestDataFactory.generateId();
        Long chargerId = TestDataFactory.generateId();
        Long userId = TestDataFactory.generateId();

        assertTrue(
            orderService.createOrderOnStart(
                stationId,
                chargerId,
                sessionId,
                userId,
                null
            )
        );
        ChargingOrder first = orderService.getBySessionId(sessionId);
        assertNotNull(first);

        assertTrue(
            orderService.createOrderOnStart(
                stationId,
                chargerId,
                sessionId,
                userId,
                null
            )
        );
        ChargingOrder second = orderService.getBySessionId(sessionId);
        assertNotNull(second);

        assertEquals(first.getId(), second.getId());
    }

    @Test
    @DisplayName("租户隔离 - 不同租户的订单应该隔离")
    void testTenantIsolation() {
        String sessionId = TestDataFactory.generateCode("SESSION");
        Long stationId = TestDataFactory.generateId();
        Long chargerId = TestDataFactory.generateId();
        Long userId = TestDataFactory.generateId();

        // Create under default tenant (DEFAULT_TENANT_ID)
        assertTrue(
            orderService.createOrderOnStart(
                stationId,
                chargerId,
                sessionId,
                userId,
                null
            )
        );
        ChargingOrder created = orderService.getBySessionId(sessionId);
        assertNotNull(created);
        assertEquals(DEFAULT_TENANT_ID, created.getTenantId());

        // Switch to another tenant; should not see the order
        switchTenant(2L);
        try {
            ChargingOrder inOtherTenant = orderService.getBySessionId(
                sessionId
            );
            assertNull(inOtherTenant);
        } finally {
            // restore tenant context for subsequent tests
            switchTenant(DEFAULT_TENANT_ID);
        }
    }
}
