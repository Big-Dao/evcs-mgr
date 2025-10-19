package com.evcs.common.tenant;

import com.evcs.common.config.TenantContextTaskDecorator;
import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Small unit tests for {@link com.evcs.common.config.TenantContextTaskDecorator}.
 *
 * These tests verify:
 *  - the submitting thread's TenantContext is visible inside decorated tasks, and
 *  - any pre-existing worker-thread TenantContext is restored after the task completes.
 *
 * The tests avoid making assertions directly on worker threads; instead they return results
 * via futures/CompletableFutures and assert on the main test thread so that failures are reported
 * properly by the JUnit runner.
 */
class TenantContextTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        // Always clear the test thread's tenant context to avoid leaks between tests
        TenantContext.clear();
    }

    @Test
    void shouldPropagateTenantContextToWorkerThread() throws Exception {
        final Long submittingTenantId = 42L;
        final Long submittingUserId = 777L;

        // Arrange: set tenant context on the submitting (test) thread
        TenantContext.setTenantId(submittingTenantId);
        TenantContext.setUserId(submittingUserId);

        TenantContextTaskDecorator decorator = new TenantContextTaskDecorator();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Use a CompletableFuture to capture assertion result from worker thread
            CompletableFuture<Boolean> insideResult = new CompletableFuture<>();

            Runnable task = () -> {
                try {
                    // Inside worker thread: tenant context should be propagated
                    boolean ok = submittingTenantId.equals(TenantContext.getTenantId())
                            && submittingUserId.equals(TenantContext.getUserId());
                    insideResult.complete(ok);
                } catch (Throwable t) {
                    insideResult.completeExceptionally(t);
                }
            };

            // Capture and decorate the task while the submitting thread's context is set
            Runnable decorated = decorator.decorate(task);

            // Act: submit decorated task to executor
            Future<?> execFuture = executor.submit(decorated);

            // Wait for the decorated task to finish and for the insideResult to be set
            execFuture.get(2, TimeUnit.SECONDS);
            assertTrue(insideResult.get(2, TimeUnit.SECONDS),
                    "TenantContext should be visible inside the decorated task");

            // After the task completes, the worker thread should not retain the captured context
            Future<Long> postRead = executor.submit(TenantContext::getTenantId);
            Long postValue = postRead.get(1, TimeUnit.SECONDS);
            assertNull(postValue, "Worker thread should not retain captured TenantContext after task completion");

            // Ensure submitting thread's context is untouched
            assertEquals(submittingTenantId, TenantContext.getTenantId(), "Submitting thread context must remain unchanged");
            assertEquals(submittingUserId, TenantContext.getUserId(), "Submitting thread user context must remain unchanged");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldRestorePreviousWorkerThreadContextAfterExecution() throws Exception {
        final Long previousTenantId = 999L;
        final Long previousUserId = 888L;
        final Long capturedTenantId = 123L;
        final Long capturedUserId = 456L;

        TenantContextTaskDecorator decorator = new TenantContextTaskDecorator();

        // Use a single-thread executor so we can reliably populate the worker thread's "previous" context
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // 1) Pre-populate the worker thread with a previous context.
            Future<?> pre = executor.submit(() -> {
                TenantContext.setTenantId(previousTenantId);
                TenantContext.setUserId(previousUserId);
            });
            pre.get(1, TimeUnit.SECONDS);

            // 2) On the submitting thread set the context that should be captured and propagated.
            TenantContext.setTenantId(capturedTenantId);
            TenantContext.setUserId(capturedUserId);

            // Use an AtomicReference to bring the verification result back to the test thread.
            AtomicReference<Boolean> insideVerified = new AtomicReference<>(false);

            Runnable task = () -> {
                // Inside the decorated task we should see the captured values (not the previous ones)
                insideVerified.set(capturedTenantId.equals(TenantContext.getTenantId())
                        && capturedUserId.equals(TenantContext.getUserId()));

                // Mutate the tenant context to simulate the task changing thread-local state;
                // the decorator should restore the previous state afterwards.
                TenantContext.setTenantId(555L);
                TenantContext.setUserId(666L);
            };

            Runnable decorated = decorator.decorate(task);

            // Act: run the decorated task on the executor
            Future<?> runFuture = executor.submit(decorated);
            runFuture.get(2, TimeUnit.SECONDS);

            // 3) After the decorated task completes, the previous context on the worker thread
            // should have been restored. Read it back via a small callable submitted to the same executor.
            Future<Long> restoredTenantFuture = executor.submit(TenantContext::getTenantId);
            Long restoredTenantId = restoredTenantFuture.get(1, TimeUnit.SECONDS);

            Future<Long> restoredUserFuture = executor.submit(TenantContext::getUserId);
            Long restoredUserId = restoredUserFuture.get(1, TimeUnit.SECONDS);

            // Assert the task observed the captured values
            assertTrue(insideVerified.get(), "Decorated task did not observe the submitting thread's TenantContext");

            // Assert the worker thread's previous context was restored
            assertEquals(previousTenantId, restoredTenantId, "Worker thread previous tenantId should be restored after task execution");
            assertEquals(previousUserId, restoredUserId, "Worker thread previous userId should be restored after task execution");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
