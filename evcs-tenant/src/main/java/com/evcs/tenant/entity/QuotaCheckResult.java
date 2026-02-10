package com.evcs.tenant.entity;

/**
 * 配额检查结果
 */
public record QuotaCheckResult(
        boolean allowed,           // 是否允许
        String reason,             // 不允许的原因
        Integer current,           // 当前使用量
        Integer limit,             // 配额限制
        String resourceType        // 资源类型
) {
    public static QuotaCheckResult ok() {
        return new QuotaCheckResult(true, null, null, null, null);
    }

    public static QuotaCheckResult denied(String reason, Integer current, Integer limit, String resourceType) {
        return new QuotaCheckResult(false, reason, current, limit, resourceType);
    }

    public static QuotaCheckResult denied(String reason) {
        return new QuotaCheckResult(false, reason, null, null, null);
    }
}
