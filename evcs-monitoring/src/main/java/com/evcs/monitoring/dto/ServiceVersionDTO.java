package com.evcs.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceVersionDTO {

    private String serviceName;

    private String instanceId;

    private String host;

    private Integer port;

    private boolean reachable;

    private String error;

    private String buildVersion;

    private String buildTime;

    private String gitCommit;

    private String imageTag;

    private String registry;
}
