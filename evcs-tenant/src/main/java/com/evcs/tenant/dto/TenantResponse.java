package com.evcs.tenant.dto;

import com.evcs.tenant.entity.SysTenant;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 租户信息响应 DTO。
 *
 * <p>对外暴露租户公开字段；{@code ancestors}（层级链）、审计人与乐观锁/逻辑删除字段
 * 属于 Entity 内部结构，不得通过 API 泄漏。
 */
@Value
@Builder
public class TenantResponse {
    Long id;
    String tenantCode;
    String tenantName;
    Long parentId;
    String parentName;
    String contactPerson;
    String contactPhone;
    String contactEmail;
    String address;
    String socialCode;
    String licenseUrl;
    Integer tenantType;
    String tenantTypeName;
    Integer status;
    LocalDateTime expireTime;
    Integer maxUsers;
    Integer maxStations;
    Integer maxChargers;
    Integer maxChildren;
    Integer maxSessions;
    String remark;
    Integer childrenCount;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static TenantResponse from(SysTenant tenant) {
        if (tenant == null) {
            return null;
        }
        return TenantResponse.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .tenantName(tenant.getTenantName())
                .parentId(tenant.getParentId())
                .parentName(tenant.getParentName())
                .contactPerson(tenant.getContactPerson())
                .contactPhone(tenant.getContactPhone())
                .contactEmail(tenant.getContactEmail())
                .address(tenant.getAddress())
                .socialCode(tenant.getSocialCode())
                .licenseUrl(tenant.getLicenseUrl())
                .tenantType(tenant.getTenantType())
                .tenantTypeName(tenant.getTenantTypeName())
                .status(tenant.getStatus())
                .expireTime(tenant.getExpireTime())
                .maxUsers(tenant.getMaxUsers())
                .maxStations(tenant.getMaxStations())
                .maxChargers(tenant.getMaxChargers())
                .maxChildren(tenant.getMaxChildren())
                .maxSessions(tenant.getMaxSessions())
                .remark(tenant.getRemark())
                .childrenCount(tenant.getChildrenCount())
                .createTime(tenant.getCreateTime())
                .updateTime(tenant.getUpdateTime())
                .build();
    }
}
