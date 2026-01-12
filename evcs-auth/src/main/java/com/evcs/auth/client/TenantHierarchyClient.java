package com.evcs.auth.client;

import com.evcs.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "evcs-tenant",
        contextId = "tenantHierarchyClient",
        path = "/internal/api/v1/tenant-hierarchy"
)
public interface TenantHierarchyClient {

    @GetMapping("/descendant")
    Result<Boolean> isDescendant(
            @RequestParam("currentTenantId") Long currentTenantId,
            @RequestParam("targetTenantId") Long targetTenantId);
}
