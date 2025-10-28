package com.evcs.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标基类
 * 提供常用的业务指标注册和管理功能
 */
@Slf4j
@Component
public abstract class BusinessMetrics {

    protected final MeterRegistry meterRegistry;

    protected BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerMetrics();
    }

    /**
     * 子类实现此方法注册具体的业务指标
     */
    protected abstract void registerMetrics();

    /**
     * 创建计数器
     * 
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 标签键值对
     * @return Counter
     */
    protected Counter createCounter(String name, String description, String... tags) {
        try {
            // 先尝试查找已存在的指标
            Counter existing = meterRegistry.find(name).tags(tags).counter();
            if (existing != null) {
                log.debug("Counter {} already exists, returning existing instance", name);
                return existing;
            }
            // 不存在则创建新的
            return Counter.builder(name)
                    .description(description)
                    .tags(tags)
                    .register(meterRegistry);
        } catch (IllegalArgumentException e) {
            // 如果 Prometheus CollectorRegistry 中已存在，尝试再次查找
            if (e.getMessage() != null && e.getMessage().contains("already in use")) {
                log.warn("Counter {} already registered in Prometheus, attempting to find existing", name);
                Counter existing = meterRegistry.find(name).tags(tags).counter();
                if (existing != null) {
                    return existing;
                }
            }
            log.error("Failed to create counter: {}", name, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to create counter: {}", name, e);
            throw e;
        }
    }

    /**
     * 创建Gauge指标
     * 
     * @param name 指标名称
     * @param description 指标描述
     * @param number 数值对象
     * @param tags 标签键值对
     */
    protected void createGauge(String name, String description, Number number, String... tags) {
        try {
            // 先检查是否已存在
            if (meterRegistry.find(name).tags(tags).gauge() != null) {
                log.debug("Gauge {} already exists, skipping registration", name);
                return;
            }
            Gauge.builder(name, number, Number::doubleValue)
                    .description(description)
                    .tags(tags)
                    .register(meterRegistry);
        } catch (Exception e) {
            log.error("Failed to create gauge: {}", name, e);
            throw e;
        }
    }

    /**
     * 创建Timer指标
     * 
     * @param name 指标名称
     * @param description 指标描述
     * @param tags 标签键值对
     * @return Timer
     */
    protected Timer createTimer(String name, String description, String... tags) {
        try {
            // 先尝试查找已存在的指标
            Timer existing = meterRegistry.find(name).tags(tags).timer();
            if (existing != null) {
                log.debug("Timer {} already exists, returning existing instance", name);
                return existing;
            }
            return Timer.builder(name)
                    .description(description)
                    .tags(tags)
                    .register(meterRegistry);
        } catch (Exception e) {
            log.error("Failed to create timer: {}", name, e);
            throw e;
        }
    }

    /**
     * 记录Counter增加
     * 
     * @param counter 计数器
     */
    protected void incrementCounter(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * 记录Counter增加指定值
     * 
     * @param counter 计数器
     * @param amount 增加量
     */
    protected void incrementCounter(Counter counter, double amount) {
        if (counter != null) {
            counter.increment(amount);
        }
    }
}
