package com.evcs.order.controller.internal;

import com.evcs.common.result.Result;
import com.evcs.order.dto.OrderUsageCount;
import com.evcs.order.service.OrderUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单资源用量内部查询端点（服务间调用）。
 *
 * <p>供 tenant 服务删除预检使用：网关在边缘封锁 /internal/api/**，
 * 服务内由 InternalApiTokenFilter 校验共享令牌。
 */
@RestController
@RequestMapping("/internal/api/v1")
@RequiredArgsConstructor
public class OrderUsageInternalController {

    private final OrderUsageService orderUsageService;

    @GetMapping("/order-usage-counts")
    public Result<List<OrderUsageCount>> usageCounts(@RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(orderUsageService.getUsageCounts(tenantIds));
    }
}
