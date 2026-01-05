package com.evcs.station.client;

import com.evcs.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "evcs-order", contextId = "orderClient", path = "/order")
public interface OrderClient {

    @PostMapping("/start")
    Result<Boolean> startOrder(@RequestParam("stationId") Long stationId,
                               @RequestParam("chargerId") Long chargerId,
                               @RequestParam("sessionId") String sessionId,
                               @RequestParam(value = "userId", required = false) Long userId,
                               @RequestParam(value = "billingPlanId", required = false) Long billingPlanId);

    @PostMapping("/stop")
    Result<Boolean> stopOrder(@RequestParam("sessionId") String sessionId,
                              @RequestParam("energy") Double energy,
                              @RequestParam("duration") Long duration);
}
