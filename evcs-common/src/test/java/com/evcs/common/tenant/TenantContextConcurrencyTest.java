package com.evcs.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tenant Context Concurrency and Isolation Tests
 * Tests for Week 1 - Day 4: Tenant Context Security
 * 
 * Validates that TenantContext properly isolates tenant data across threads
 * and prevents cross-tenant data leaks in concurrent scenarios.
 */
class TenantContextConcurrencyTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void testBasicThreadLocalIsolation() {
        // Set tenant in main thread
        TenantContext.setTenantId(100L);
        TenantContext.setUserId(200L);

        assertEquals(100L, TenantContext.getTenantId());
        assertEquals(200L, TenantContext.getUserId());

        // Create new thread and verify it doesn't see main thread's context
        Thread thread = new Thread(() -> {
            assertNull(TenantContext.getTenantId(), "New thread should not inherit tenant context");
            assertNull(TenantContext.getUserId(), "New thread should not inherit user context");

            // Set different values in new thread
            TenantContext.setTenantId(999L);
            TenantContext.setUserId(888L);

            assertEquals(999L, TenantContext.getTenantId());
            assertEquals(888L, TenantContext.getUserId());
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        // Verify main thread context is unchanged
        assertEquals(100L, TenantContext.getTenantId());
        assertEquals(200L, TenantContext.getUserId());
    }

    @Test
    void testConcurrentTenantIsolation() throws InterruptedException {
        int threadCount = 100;
        int requestsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final long tenantId = i + 1;
            final long userId = (i + 1) * 100;

            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    for (int req = 0; req < requestsPerThread; req++) {
                        try {
                            // Simulate request processing
                            TenantContext.setTenantId(tenantId);
                            TenantContext.setUserId(userId);

                            // Simulate some work
                            Thread.sleep(1);

                            // Verify tenant isolation
                            Long currentTenantId = TenantContext.getTenantId();
                            Long currentUserId = TenantContext.getUserId();

                            if (currentTenantId == null || currentTenantId != tenantId) {
                                String error = String.format(
                                    "Tenant leak detected! Expected tenant %d but got %s",
                                    tenantId, currentTenantId
                                );
                                errors.add(error);
                                errorCount.incrementAndGet();
                            }

                            if (currentUserId == null || currentUserId != userId) {
                                String error = String.format(
                                    "User leak detected! Expected user %d but got %s",
                                    userId, currentUserId
                                );
                                errors.add(error);
                                errorCount.incrementAndGet();
                            }
                        } finally {
                            // Simulate cleanup after request
                            TenantContext.clear();
                        }
                    }
                } catch (Exception e) {
                    errors.add("Exception: " + e.getMessage());
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test should complete within timeout");
        
        if (errorCount.get() > 0) {
            errors.forEach(System.err::println);
            fail(String.format("Found %d tenant isolation errors. First 10 errors logged above.", 
                               errorCount.get()));
        }
    }

    @Test
    void testContextCleanupPreventsLeaks() throws InterruptedException {
        int iterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(iterations);
        AtomicInteger leakCount = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            final long tenantId = i % 100 + 1;

            executor.submit(() -> {
                try {
                    // Simulate request without cleanup
                    TenantContext.setTenantId(tenantId);
                    
                    // Simulate work
                    Thread.sleep(5);
                    
                    // Verify context is set
                    assertNotNull(TenantContext.getTenantId());
                    
                    // Clear context
                    TenantContext.clear();
                    
                    // Verify context is cleared
                    if (TenantContext.getTenantId() != null) {
                        leakCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test should complete within timeout");
        assertEquals(0, leakCount.get(), "No context leaks should occur after cleanup");
    }

    @Test
    void testContextInfoDebugMethod() {
        TenantContext.setTenantId(123L);
        TenantContext.setUserId(456L);
        TenantContext.setTenantType(1);
        TenantContext.setTenantAncestors(",1,2,3,");

        String contextInfo = TenantContext.getContextInfo();

        assertNotNull(contextInfo);
        assertTrue(contextInfo.contains("tenantId=123"));
        assertTrue(contextInfo.contains("userId=456"));
        assertTrue(contextInfo.contains("tenantType=1"));
        assertTrue(contextInfo.contains("ancestors=,1,2,3,"));
    }

    @Test
    void testSystemAdminCheck() {
        // Non-admin tenant
        TenantContext.setTenantType(2);
        assertFalse(TenantContext.isSystemAdmin());

        // System admin tenant
        TenantContext.setTenantType(1);
        assertTrue(TenantContext.isSystemAdmin());

        // Null tenant type
        TenantContext.clear();
        assertFalse(TenantContext.isSystemAdmin());
    }

    @Test
    void testHasAccessToTenant() {
        // Setup current tenant
        TenantContext.setTenantId(100L);
        TenantContext.setTenantAncestors(",50,100,");

        // Can access own tenant
        assertTrue(TenantContext.hasAccessToTenant(100L));

        // Can access child tenant (in ancestors path)
        assertTrue(TenantContext.hasAccessToTenant(50L));

        // Cannot access unrelated tenant
        assertFalse(TenantContext.hasAccessToTenant(200L));

        // System admin can access all
        TenantContext.setTenantType(1);
        assertTrue(TenantContext.hasAccessToTenant(999L));
    }

    @Test
    void testClearRemovesAllContext() {
        // Set all context values
        TenantContext.setTenantId(123L);
        TenantContext.setUserId(456L);
        TenantContext.setTenantType(2);
        TenantContext.setTenantAncestors(",1,2,3,");

        // Verify all are set
        assertNotNull(TenantContext.getTenantId());
        assertNotNull(TenantContext.getUserId());
        assertNotNull(TenantContext.getTenantType());
        assertNotNull(TenantContext.getTenantAncestors());

        // Clear context
        TenantContext.clear();

        // Verify all are cleared
        assertNull(TenantContext.getTenantId());
        assertNull(TenantContext.getUserId());
        assertNull(TenantContext.getTenantType());
        assertNull(TenantContext.getTenantAncestors());
    }

    @Test
    void testThreadPoolScenario() throws InterruptedException, ExecutionException {
        // Simulate thread pool reuse scenario
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Submit 20 tasks to 5 threads
        for (int i = 0; i < 20; i++) {
            final long tenantId = i + 1;
            
            Future<Boolean> future = threadPool.submit(() -> {
                try {
                    // Verify thread starts clean
                    assertNull(TenantContext.getTenantId(), 
                              "Thread should start with clean context");

                    // Set tenant context
                    TenantContext.setTenantId(tenantId);
                    
                    // Do work
                    Thread.sleep(10);
                    
                    // Verify context is still correct
                    assertEquals(tenantId, TenantContext.getTenantId());
                    
                    return true;
                } catch (AssertionError | InterruptedException e) {
                    return false;
                } finally {
                    // Always cleanup
                    TenantContext.clear();
                }
            });
            
            futures.add(future);
        }

        // Verify all tasks completed successfully
        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "All tasks should complete successfully");
        }

        threadPool.shutdown();
        assertTrue(threadPool.awaitTermination(5, TimeUnit.SECONDS));
    }
}
