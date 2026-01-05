package com.evcs.gateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

@Configuration
public class ReactorSchedulerConfig {

    /**
     * Managed Reactor Scheduler for explicit thread switching.
     *
     * Prefer injecting this Scheduler and using {@code publishOn(..)}/{@code subscribeOn(..)}
     * with it instead of using {@code Schedulers.parallel()/boundedElastic()}.
     *
     * The backing executor is managed and decorated to propagate TenantContext and MDC.
     *
     * Minimal usage example (copy-paste):
     * <pre>
     * {@code
     * @Component
     * class ExampleReactiveUsage {
     *
     *     private final Scheduler evcsReactorScheduler;
     *
     *     ExampleReactiveUsage(Scheduler evcsReactorScheduler) {
     *         this.evcsReactorScheduler = evcsReactorScheduler;
     *     }
     *
     *     Mono<String> doWork() {
     *         return Mono.fromCallable(() -> "ok")
     *             .publishOn(evcsReactorScheduler);
     *     }
     * }
     * }
     * </pre>
     */
    @Bean(destroyMethod = "dispose")
    public Scheduler evcsReactorScheduler(
        @Qualifier("chargingExecutor") Executor chargingExecutor
    ) {
        return Schedulers.fromExecutor(chargingExecutor);
    }
}
