package com.evcs.order.config;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cache preload configuration for hot stations.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "evcs.order.cache")
public class OrderCachePreloadProperties {

    @NotNull
    private List<Long> hotStations = List.of(1L, 2L, 3L, 4L, 5L);
}
