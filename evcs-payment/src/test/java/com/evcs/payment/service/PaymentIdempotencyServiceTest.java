package com.evcs.payment.service;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.impl.PaymentIdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付幂等性服务测试
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class PaymentIdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private PaymentOrderMapper paymentOrderMapper;

    @Mock
    private PaymentMetrics paymentMetrics;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PaymentIdempotencyServiceImpl idempotencyService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new PaymentIdempotencyServiceImpl(redisTemplate, paymentOrderMapper, paymentMetrics);
    }

    @Test
    void testTryLock_Success() {
        // Given
        String idempotentKey = "test_key_123";
        String requestId = "request_456";
        long lockTime = 30L;

        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // When
        boolean result = idempotencyService.tryLock(idempotentKey, requestId, lockTime);

        // Then
        assertTrue(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).setIfAbsent("payment:lock:test_key_123", requestId, 30L, TimeUnit.SECONDS);
        verify(paymentMetrics).recordCustomMetric(eq("payment.idempotency.lock.success"), eq(1.0), any(Map.class));
    }

    @Test
    void testTryLock_Failure() {
        // Given
        String idempotentKey = "test_key_123";
        String requestId = "request_456";
        long lockTime = 30L;

        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // When
        boolean result = idempotencyService.tryLock(idempotentKey, requestId, lockTime);

        // Then
        assertFalse(result);
        verify(paymentMetrics).recordCustomMetric(eq("payment.idempotency.lock.failure"), eq(1.0), any(Map.class));
    }

    @Test
    void testUnlock_Success() {
        // Given
        String idempotentKey = "test_key_123";
        String requestId = "request_456";

        // When
        idempotencyService.unlock(idempotentKey, requestId);

        // Then
        // unlock method uses Lua script through RedisTemplate.execute
        // The test just verifies that no exceptions are thrown
        // In a real test environment, Redis would handle the script execution
    }

    @Test
    void testGetExistingPayment_FromCache() {
        // Given
        String idempotentKey = "test_key_123";
        PaymentOrder cachedOrder = new PaymentOrder();
        cachedOrder.setId(1L);
        cachedOrder.setTradeNo("trade_123");

        when(valueOperations.get("payment:cache:test_key_123")).thenReturn(cachedOrder);

        // When
        PaymentOrder result = idempotencyService.getExistingPayment(idempotentKey);

        // Then
        assertNotNull(result);
        assertEquals("trade_123", result.getTradeNo());
        // verify(paymentMetrics).recordIdempotencyMetrics(eq(idempotentKey), eq(true), anyLong()); // Method doesn't exist in PaymentMetrics
        verify(paymentOrderMapper, never()).selectOne(any());
    }

    @Test
    void testGetExistingPayment_FromDatabase() {
        // Given
        String idempotentKey = "test_key_123";
        PaymentOrder dbOrder = new PaymentOrder();
        dbOrder.setId(1L);
        dbOrder.setTradeNo("trade_123");

        when(valueOperations.get("payment:cache:test_key_123")).thenReturn(null);
        when(paymentOrderMapper.selectOne(any())).thenReturn(dbOrder);

        // When
        PaymentOrder result = idempotencyService.getExistingPayment(idempotentKey);

        // Then
        assertNotNull(result);
        assertEquals("trade_123", result.getTradeNo());
        verify(paymentOrderMapper).selectOne(any());
        verify(valueOperations).set(eq("payment:cache:test_key_123"), eq(dbOrder), eq(24L), eq(TimeUnit.HOURS));
        // verify(paymentMetrics).recordIdempotencyMetrics(eq(idempotentKey), eq(false), anyLong()); // Method doesn't exist in PaymentMetrics
    }

    @Test
    void testGetExistingPayment_NotFound() {
        // Given
        String idempotentKey = "test_key_123";

        when(valueOperations.get("payment:cache:test_key_123")).thenReturn(null);
        when(paymentOrderMapper.selectOne(any())).thenReturn(null);

        // When
        PaymentOrder result = idempotencyService.getExistingPayment(idempotentKey);

        // Then
        assertNull(result);
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void testCachePaymentResult() {
        // Given
        String idempotentKey = "test_key_123";
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(1L);

        // When
        idempotencyService.cachePaymentResult(idempotentKey, paymentOrder, 24L, TimeUnit.HOURS);

        // Then
        verify(valueOperations).set("payment:cache:test_key_123", paymentOrder, 24L, TimeUnit.HOURS);
        verify(paymentMetrics).recordCustomMetric(eq("payment.idempotency.cache.set"), eq(1.0), any(Map.class));
    }

    @Test
    void testGetCachedPaymentResult() {
        // Given
        String idempotentKey = "test_key_123";
        PaymentOrder cachedOrder = new PaymentOrder();
        cachedOrder.setId(1L);

        when(valueOperations.get("payment:cache:test_key_123")).thenReturn(cachedOrder);

        // When
        PaymentOrder result = idempotencyService.getCachedPaymentResult(idempotentKey);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGenerateIdempotentKey_FromRequest() {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(123L);
        request.setUserId(456L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        request.setIdempotentKey("custom_key_123");

        // When
        String result = idempotencyService.generateIdempotentKey(request);

        // Then
        assertEquals("custom_key_123", result);
    }

    @Test
    void testGenerateIdempotentKey_AutoGenerate() {
        // Given
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(123L);
        request.setUserId(456L);
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.ALIPAY_APP);

        // When
        String result = idempotencyService.generateIdempotentKey(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.length() <= 32);
    }

    @Test
    void testValidateIdempotentKey_Valid() {
        // Given
        String validKey = "valid_key_123_ABC";

        // When
        boolean result = idempotencyService.validateIdempotentKey(validKey);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateIdempotentKey_Invalid_TooLong() {
        // Given
        String longKey = "a".repeat(65);

        // When
        boolean result = idempotencyService.validateIdempotentKey(longKey);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateIdempotentKey_Invalid_Characters() {
        // Given
        String invalidKey = "invalid@key#123";

        // When
        boolean result = idempotencyService.validateIdempotentKey(invalidKey);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateIdempotentKey_NullOrEmpty() {
        // When & Then
        assertFalse(idempotencyService.validateIdempotentKey(null));
        assertFalse(idempotencyService.validateIdempotentKey(""));
        assertFalse(idempotencyService.validateIdempotentKey("   "));
    }

    @Test
    void testIsDuplicateRequest_True() {
        // Given
        String idempotentKey = "test_key_123";
        String requestId = "request_456";

        when(valueOperations.get("payment:lock:test_key_123")).thenReturn(requestId);

        // When
        boolean result = idempotencyService.isDuplicateRequest(idempotentKey, requestId);

        // Then
        assertTrue(result);
        verify(paymentMetrics).recordCustomMetric(eq("payment.idempotency.duplicate.request"), eq(1.0), any(Map.class));
    }

    @Test
    void testIsDuplicateRequest_False() {
        // Given
        String idempotentKey = "test_key_123";
        String requestId = "request_456";

        when(valueOperations.get("payment:lock:test_key_123")).thenReturn("different_request");

        // When
        boolean result = idempotencyService.isDuplicateRequest(idempotentKey, requestId);

        // Then
        assertFalse(result);
    }

    @Test
    void testCleanExpiredCache() {
        // Given
        java.time.LocalDateTime expireBefore = java.time.LocalDateTime.now().minusDays(1);

        // When
        int result = idempotencyService.cleanExpiredCache(expireBefore);

        // Then
        assertEquals(0, result); // Redis handles TTL automatically
    }

    @Test
    void testRecordIdempotencyMetrics() {
        // Given
        String idempotentKey = "test_key_123";
        boolean hit = true;
        long processTime = 100L;

        // When
        idempotencyService.recordIdempotencyMetrics(idempotentKey, hit, processTime);

        // Then
        verify(paymentMetrics).recordCustomMetric(eq("payment.idempotency.cache.hit"), eq(1.0), any(Map.class));
        verify(paymentMetrics).recordTimer(eq("payment.idempotency.process.duration"), eq(100L), any(Map.class));
    }
}