package com.evcs.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户创建/更新请求 DTO。
 *
 * <p>仅暴露调用方可写的业务字段：id/ancestors/version/deleted/审计人等
 * 内部字段不出现在绑定面，杜绝批量赋值（mass-assignment）注入。
 * ancestors 由服务根据 parentId 计算，不接受外部输入。
 */
@Data
public class TenantUpsertRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    /**
     * 父租户ID（null/0 表示顶级租户）
     */
    private Long parentId;

    @NotBlank(message = "联系人不能为空")
    private String contactPerson;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    private String contactEmail;
    private String address;
    private String socialCode;
    private String licenseUrl;

    /**
     * 租户类型：1-平台方，2-运营商，3-第三方合作伙伴
     */
    private Integer tenantType;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    private LocalDateTime expireTime;
    private Integer maxUsers;
    private Integer maxStations;
    private Integer maxChargers;
    private Integer maxChildren;
    private Integer maxSessions;
    private String remark;

    public com.evcs.tenant.entity.SysTenant toEntity() {
        com.evcs.tenant.entity.SysTenant tenant = new com.evcs.tenant.entity.SysTenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(tenantName);
        tenant.setParentId(parentId);
        tenant.setContactPerson(contactPerson);
        tenant.setContactPhone(contactPhone);
        tenant.setContactEmail(contactEmail);
        tenant.setAddress(address);
        tenant.setSocialCode(socialCode);
        tenant.setLicenseUrl(licenseUrl);
        tenant.setTenantType(tenantType);
        tenant.setStatus(status);
        tenant.setExpireTime(expireTime);
        tenant.setMaxUsers(maxUsers);
        tenant.setMaxStations(maxStations);
        tenant.setMaxChargers(maxChargers);
        tenant.setMaxChildren(maxChildren);
        tenant.setMaxSessions(maxSessions);
        tenant.setRemark(remark);
        return tenant;
    }
}
