package com.evcs.common.executor;

import com.evcs.common.tenant.TenantContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExecutorService wrapper that propagates the current thread's {@link TenantContext}
 * into tasks submitted to an underlying {@link ExecutorService}.
 *
 * <p>
 * Usage:
 * <pre>
 * ExecutorService raw = Executors.newFixedThreadPool(10);
 * ExecutorService wrapped = new TenantContextPropagatingExecutorService(raw);
 *
 * // When a submitting thread has TenantContext set, the context will be visible inside
 * // the executed Runnable/Callable on the worker thread.
 * </pre>
 * </p>
 *
 * Behavior:
 * - Captures a snapshot of tenant-related thread-local state at submit time.
 * - Applies the snapshot on the worker thread before task execution.
 * - Always clears and restores any pre-existing worker-thread context after task execution
 *   to avoid leaking tenant state between tasks.
 *
 * Thread-safety: this class is a thin, thread-safe wrapper delegating concurrency to the
 * provided {@code ExecutorService}.
 */
public class TenantContextPropagatingExecutorService
    implements ExecutorService {

    private static final Logger log = LoggerFactory.getLogger(
        TenantContextPropagatingExecutorService.class
    );

    private final ExecutorService delegate;

    /**
     * Wrap the given {@link ExecutorService}.
     *
     * @param delegate non-null delegate executor to wrap
     */
    public TenantContextPropagatingExecutorService(ExecutorService delegate) {
        this.delegate = Objects.requireNonNull(
            delegate,
            "delegate ExecutorService must not be null"
        );
    }

    /**
     * Convenience factory that avoids double-wrapping.
     */
    public static ExecutorService wrap(ExecutorService delegate) {
        if (delegate instanceof TenantContextPropagatingExecutorService) {
            return delegate;
        }
        return new TenantContextPropagatingExecutorService(delegate);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrapRunnable(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrapRunnable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrapRunnable(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrapCallable(task));
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    // --- invokeAll / invokeAny: wrap each submitted callable with context propagation ---

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks
    ) throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(
        Collection<? extends Callable<T>> tasks,
        long timeout,
        TimeUnit unit
    ) throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapCallables(tasks));
    }

    @Override
    public <T> T invokeAny(
        Collection<? extends Callable<T>> tasks,
        long timeout,
        TimeUnit unit
    ) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
    }

    // --- Helper methods to wrap tasks ---

    /**
     * Wrap a Runnable so that the submitting thread's TenantContext is applied when the runnable runs,
     * and the worker thread's previous TenantContext is restored afterwards.
     */
    protected Runnable wrapRunnable(final Runnable runnable) {
        final TenantContextSnapshot captured = TenantContextSnapshot.capture();
        return () -> {
            final TenantContextSnapshot previous =
                TenantContextSnapshot.capture();
            try {
                captured.apply();
                runnable.run();
            } finally {
                // Clear to avoid leaking the captured context, then restore any previous context.
                TenantContext.clear();
                previous.apply();
            }
        };
    }

    /**
     * Wrap a Callable so that the submitting thread's TenantContext is applied when the callable runs,
     * and the worker thread's previous TenantContext is restored afterwards.
     */
    protected <T> Callable<T> wrapCallable(final Callable<T> callable) {
        final TenantContextSnapshot captured = TenantContextSnapshot.capture();

        // Return a Callable that:
        // 1) Applies the submitting thread's TenantContext snapshot before execution
        // 2) Executes the original callable and captures either its result or any throwable it throws
        // 3) Always attempts to clear and restore the worker thread's previous context
        // 4) Ensures that a successful result is preserved even if context restoration fails;
        //    if the task threw, any restoration failure is added as a suppressed exception.
        return () -> {
            final TenantContextSnapshot previous =
                TenantContextSnapshot.capture();
            T result = null;
            Throwable thrown = null;

            try {
                captured.apply();
                result = callable.call();
            } catch (Throwable t) {
                // Capture the throwable so we can try to restore context and then rethrow it.
                thrown = t;
            } finally {
                try {
                    // Always clear the captured context and attempt to restore previous state.
                    TenantContext.clear();
                    previous.apply();
                } catch (Throwable restoreEx) {
                    if (thrown != null) {
                        // If the task already threw, attach restore failure as suppressed so the original
                        // failure is preserved for callers while still surfacing restoration problems.
                        thrown.addSuppressed(restoreEx);
                    } else {
                        // If the task succeeded but restoration failed, log the restoration failure
                        // but do not override the successful result (preserve return value).
                        log.error(
                            "Failed to restore TenantContext after callable completion",
                            restoreEx
                        );
                    }
                }
            }

            // If the callable threw, propagate the original throwable (with any suppressed restore issues).
            if (thrown != null) {
                if (thrown instanceof Error) {
                    throw (Error) thrown;
                } else if (thrown instanceof RuntimeException) {
                    throw (RuntimeException) thrown;
                } else if (thrown instanceof Exception) {
                    throw (Exception) thrown;
                } else {
                    throw new RuntimeException(thrown);
                }
            }

            // Task completed successfully; return the captured result.
            return result;
        };
    }

    /**
     * Wrap a collection of callables, returning a new list of wrapped callables preserving order.
     */
    protected <T> List<Callable<T>> wrapCallables(
        Collection<? extends Callable<T>> tasks
    ) {
        List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
        for (Callable<T> t : tasks) {
            wrapped.add(wrapCallable(t));
        }
        return wrapped;
    }

    /**
     * Small, immutable snapshot of the tenant-related thread-local state.
     *
     * Semantics:
     * - capture(): capture current thread state
     * - apply(): clear the current thread's TenantContext and set the captured non-null values
     *
     * Clearing first guarantees that missing fields in a snapshot will result in cleared thread-local
     * values rather than accidentally retaining stale values.
     */
    private static final class TenantContextSnapshot {

        private final Long tenantId;
        private final Long userId;
        private final Integer tenantType;
        private final String tenantAncestors;

        private TenantContextSnapshot(
            Long tenantId,
            Long userId,
            Integer tenantType,
            String tenantAncestors
        ) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.tenantType = tenantType;
            this.tenantAncestors = tenantAncestors;
        }

        static TenantContextSnapshot capture() {
            return new TenantContextSnapshot(
                TenantContext.getTenantId(),
                TenantContext.getUserId(),
                TenantContext.getTenantType(),
                TenantContext.getTenantAncestors()
            );
        }

        /**
         * Apply this snapshot to the current thread:
         * - first clears any existing context on the thread
         * - then sets any non-null values from the snapshot
         */
        void apply() {
            TenantContext.clear();

            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            if (userId != null) {
                TenantContext.setUserId(userId);
            }
            if (tenantType != null) {
                TenantContext.setTenantType(tenantType);
            }
            if (tenantAncestors != null) {
                TenantContext.setTenantAncestors(tenantAncestors);
            }
        }

        @Override
        public String toString() {
            return (
                "TenantContextSnapshot{" +
                "tenantId=" +
                tenantId +
                ", userId=" +
                userId +
                ", tenantType=" +
                tenantType +
                ", tenantAncestors='" +
                tenantAncestors +
                '\'' +
                '}'
            );
        }
    }

    /**
     * Expose the underlying delegate. Useful for shutdown/monitoring or when explicit access is required.
     */
    public ExecutorService getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return (
            "TenantContextPropagatingExecutorService{" +
            "delegate=" +
            delegate +
            '}'
        );
    }
}
