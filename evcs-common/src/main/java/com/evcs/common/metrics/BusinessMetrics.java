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
        return Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
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
        Gauge.builder(name, number, Number::doubleValue)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
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
        return Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
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
