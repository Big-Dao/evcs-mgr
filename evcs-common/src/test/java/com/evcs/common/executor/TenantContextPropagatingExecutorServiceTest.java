package com.evcs.common.executor;

import com.evcs.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TenantContextPropagatingExecutorService}.
 *
 * Tests verify:
 *  - the submitting thread's TenantContext is visible inside tasks executed by the wrapped executor;
 *  - a worker thread's previous TenantContext (if any) is restored after a wrapped task completes;
 *  - restoration semantics hold even when the task throws an exception;
 *  - concurrent submissions from multiple submitting threads do not cause cross-tenant leaks.
 */
class TenantContextPropagatingExecutorServiceTest {

    @AfterEach
    void tearDown() {
        // Ensure the test thread does not keep any tenant context between tests.
        TenantContext.clear();
    }

    @Test
    void shouldPropagateContextToWorkerThreadAndNotLeak() throws Exception {
        // Arrange - set context on the submitting (test) thread
        TenantContext.setTenantId(55L);
        TenantContext.setUserId(999L);

        ExecutorService raw = Executors.newSingleThreadExecutor();
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(raw);

        try {
            // Act - run a task via the wrapped executor and assert the context was propagated
            Future<Boolean> inside = wrapped.submit(() -> {
                Long id = TenantContext.getTenantId();
                Long user = TenantContext.getUserId();
                return id != null && id == 55L && user != null && user == 999L;
            });

            assertTrue(inside.get(2, TimeUnit.SECONDS), "Wrapped task should see submitting thread's TenantContext");

            // After the wrapped task completes, the worker thread should not retain the captured context.
            // Use the raw executor (delegate) to read the worker thread state directly.
            Future<Long> post = raw.submit(TenantContext::getTenantId);
            Long postValue = post.get(1, TimeUnit.SECONDS);
            assertNull(postValue, "Worker thread should not retain captured TenantContext after task completion");

            // Submitting thread's context must remain unchanged
            assertEquals(55L, TenantContext.getTenantId());
            assertEquals(999L, TenantContext.getUserId());
        } finally {
            wrapped.shutdownNow();
            raw.shutdownNow();
            raw.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldRestorePreviousWorkerThreadContextAfterExecution() throws Exception {
        ExecutorService raw = Executors.newSingleThreadExecutor();
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(raw);

        try {
            // Pre-populate the worker thread with a previous context using the raw executor.
            final Long previousTenantId = 777L;
            final Long previousUserId = 888L;
            Future<?> pre = raw.submit(() -> {
                TenantContext.setTenantId(previousTenantId);
                TenantContext.setUserId(previousUserId);
            });
            pre.get(1, TimeUnit.SECONDS);

            // On the submitting thread set the context that should be captured and propagated.
            final Long capturedTenantId = 123L;
            final Long capturedUserId = 444L;
            TenantContext.setTenantId(capturedTenantId);
            TenantContext.setUserId(capturedUserId);

            // Submit a wrapped task that observes the captured context and mutates it.
            Future<Boolean> run = wrapped.submit(() -> {
                Long seenTenant = TenantContext.getTenantId();
                Long seenUser = TenantContext.getUserId();
                boolean observed = seenTenant != null && seenTenant == capturedTenantId
                        && seenUser != null && seenUser == capturedUserId;

                // Mutate thread-local to simulate task-side changes.
                TenantContext.setTenantId(555L);
                TenantContext.setUserId(666L);
                return observed;
            });

            assertTrue(run.get(2, TimeUnit.SECONDS), "Task should observe the captured submitting-thread TenantContext");

            // After the task completes, verify the worker thread's previous context was restored.
            Future<Long> restoredTenant = raw.submit(TenantContext::getTenantId);
            Future<Long> restoredUser = raw.submit(TenantContext::getUserId);

            assertEquals(previousTenantId, restoredTenant.get(1, TimeUnit.SECONDS));
            assertEquals(previousUserId, restoredUser.get(1, TimeUnit.SECONDS));
        } finally {
            wrapped.shutdownNow();
            raw.shutdownNow();
            raw.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldRestorePreviousWorkerThreadContextEvenIfTaskThrows() throws Exception {
        ExecutorService raw = Executors.newSingleThreadExecutor();
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(raw);

        try {
            // Pre-populate worker thread with a known context.
            final Long previousTenantId = 42L;
            final Long previousUserId = 21L;
            raw.submit(() -> {
                TenantContext.setTenantId(previousTenantId);
                TenantContext.setUserId(previousUserId);
            }).get(1, TimeUnit.SECONDS);

            // Submitting thread context to be propagated
            TenantContext.setTenantId(99L);
            TenantContext.setUserId(11L);

            // Submit a task that throws an exception
            Future<?> thrownFuture = wrapped.submit(() -> {
                throw new IllegalStateException("expected error");
            });

            // The thrown exception should propagate as an ExecutionException
            try {
                thrownFuture.get(2, TimeUnit.SECONDS);
                fail("Expected an exception from the wrapped task");
            } catch (ExecutionException ex) {
                assertTrue(ex.getCause() instanceof IllegalStateException);
            }

            // Even though task threw, the worker thread's previous context must be restored.
            Future<Long> restoredTenant = raw.submit(TenantContext::getTenantId);
            Future<Long> restoredUser = raw.submit(TenantContext::getUserId);

            assertEquals(previousTenantId, restoredTenant.get(1, TimeUnit.SECONDS));
            assertEquals(previousUserId, restoredUser.get(1, TimeUnit.SECONDS));
        } finally {
            wrapped.shutdownNow();
            raw.shutdownNow();
            raw.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldSupportConcurrentSubmissionsWithoutCrossTenantLeaks() throws Exception {
        final int submitterCount = 8;
        final int tasksPerSubmitter = 25;
        final int workerThreads = 4;

        ExecutorService raw = Executors.newFixedThreadPool(workerThreads);
        ExecutorService wrapped = TenantContextPropagatingExecutorService.wrap(raw);
        ExecutorService submitterPool = Executors.newFixedThreadPool(submitterCount);

        try {
            List<Future<Boolean>> submitterFutures = new ArrayList<>();

            for (int s = 0; s < submitterCount; s++) {
                final long tenantId = s + 1L;
                submitterFutures.add(submitterPool.submit(() -> {
                    // Each submitter sets its own TenantContext before submitting tasks.
                    TenantContext.setTenantId(tenantId);
                    try {
                        for (int t = 0; t < tasksPerSubmitter; t++) {
                            Future<Boolean> taskFuture = wrapped.submit(() -> {
                                Long seen = TenantContext.getTenantId();
                                return seen != null && seen == tenantId;
                            });

                            // Wait for the task and assert it observed the correct context.
                            try {
                                if (!taskFuture.get(5, TimeUnit.SECONDS)) {
                                    return false;
                                }
                            } catch (TimeoutException | ExecutionException ie) {
                                return false;
                            }
                        }
                        return true;
                    } finally {
                        // Ensure submitter thread clears its context after submissions.
                        TenantContext.clear();
                    }
                }));
            }

            // Wait for all submitters to complete and verify all returned true.
            submitterPool.shutdown();
            assertTrue(submitterPool.awaitTermination(1, TimeUnit.MINUTES),
                    "Submitter pool should terminate in a timely manner");

            for (Future<Boolean> f : submitterFutures) {
                assertTrue(f.get(10, TimeUnit.SECONDS), "Submitter reported a cross-tenant leak or unexpected failure");
            }

            // Finally ensure worker threads do not leak a tenant context after all tasks complete.
            Future<Long> post = raw.submit(TenantContext::getTenantId);
            assertNull(post.get(1, TimeUnit.SECONDS), "Worker threads should not retain tenant context after work completes");
        } finally {
            submitterPool.shutdownNow();
            wrapped.shutdownNow();
            raw.shutdownNow();
            raw.awaitTermination(1, TimeUnit.SECONDS);
        }
    }
}
