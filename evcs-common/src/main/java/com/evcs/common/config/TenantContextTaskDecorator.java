package com.evcs.common.config;

import com.evcs.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * TaskDecorator that propagates the current thread's {@link TenantContext} into async tasks.
 *
 * <p>
 * Thread pools reuse threads — without propagation an async task may run with a stale or missing
 * tenant context. This decorator captures the submitting thread's tenant context at decoration-time
 * and applies it to the worker thread when the task runs. It also preserves and restores any
 * pre-existing context on the worker thread to avoid interfering with other tasks.
 * </p>
 *
 * <p>Usage:
 * <pre>
 * // Example bean registration for a ThreadPoolTaskExecutor
 * // (place this in your Executor configuration class)
 * //
 * // &#64;Bean("chargingExecutor")
 * // public ThreadPoolTaskExecutor chargingExecutor(TenantContextTaskDecorator decorator) {
 * //     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 * //     executor.setCorePoolSize(10);
 * //     executor.setMaxPoolSize(50);
 * //     executor.setQueueCapacity(1000);
 * //     executor.setThreadNamePrefix("charging-exec-");
 * //     executor.setTaskDecorator(decorator);
 * //     executor.initialize();
 * //     return executor;
 * // }
 * </pre>
 * </p>
 *
 * Note: registering the decorator on the {@link org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor}
 * is necessary for Spring's @Async and other async executors that delegate to the task executor.
 */
@Component
public class TenantContextTaskDecorator implements TaskDecorator {

    private static final Logger log = LoggerFactory.getLogger(
        TenantContextTaskDecorator.class
    );

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture the submitting (caller) thread's tenant context at the moment the task is scheduled.
        final TenantContextSnapshot captured = TenantContextSnapshot.capture();

        // Return a wrapper that applies the captured context when the task runs and restores the
        // worker thread's previous context afterwards.
        return () -> {
            // Snapshot the current worker thread context so we can restore it later.
            final TenantContextSnapshot previous =
                TenantContextSnapshot.capture();

            try {
                // Apply captured context for this task (this will clear first, then set values).
                captured.apply();
                if (log.isTraceEnabled()) {
                    log.trace(
                        "TenantContext propagated to async task: {}",
                        TenantContext.getContextInfo()
                    );
                }

                // Execute the actual task while the captured context is in place.
                runnable.run();
            } finally {
                // Always clear to avoid leaking the captured context to subsequent tasks.
                TenantContext.clear();

                // Restore any previous context that existed on the worker thread.
                previous.apply();
                if (log.isTraceEnabled()) {
                    log.trace(
                        "TenantContext restored after async task: {}",
                        TenantContext.getContextInfo()
                    );
                }
            }
        };
    }

    /**
     * Lightweight snapshot of the tenant-related thread-local state.
     *
     * <p>Null fields represent "no value set" and when applying a snapshot we clear thread-local
     * storage first so that missing (null) fields result in cleared state rather than leaving
     * stale values behind.</p>
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

        /**
         * Capture the current thread's tenant context.
         */
        static TenantContextSnapshot capture() {
            return new TenantContextSnapshot(
                TenantContext.getTenantId(),
                TenantContext.getUserId(),
                TenantContext.getTenantType(),
                TenantContext.getTenantAncestors()
            );
        }

        /**
         * Apply this snapshot to the current thread.
         *
         * Behavior:
         * - Clears existing TenantContext
         * - Sets any non-null snapshot values
         *
         * This ensures the thread will not retain stale tenant values when applying a snapshot.
         */
        void apply() {
            // Clear any existing values to avoid accidental retention of stale values.
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
}
