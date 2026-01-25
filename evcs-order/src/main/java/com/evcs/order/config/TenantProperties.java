package com.evcs.order.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tenant related configuration.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "evcs.tenant")
public class TenantProperties {

    @NotNull
    @Min(1)
    private Long defaultTenantId = 1L;
}
