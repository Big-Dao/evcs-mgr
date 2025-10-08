package com.evcs.station.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.station.entity.Charger;
import com.evcs.station.service.IChargerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "计费计划分配", description = "为充电桩分配计费计划")
@RestController
@RequestMapping("/billing/assign")
@RequiredArgsConstructor
public class BillingAssignController {
    private final IChargerService chargerService;

    @PostMapping("/charger/{chargerId}")
    @Operation(summary = "为充电桩设置计费计划")
    @DataScope
    public Result<Boolean> assignPlan(@PathVariable Long chargerId, @RequestParam Long planId) {
        Charger patch = new Charger();
        patch.setChargerId(chargerId);
        patch.setBillingPlanId(planId);
        return Result.success(chargerService.updateById(patch));
    }

    @PostMapping("/station/{stationId}")
    @Operation(summary = "为站点全部充电桩设置计费计划")
    @DataScope
    public Result<Boolean> assignPlanForStation(@PathVariable Long stationId, @RequestParam Long planId) {
        java.util.List<Charger> chargers = chargerService.getChargersByStationId(stationId);
        if (chargers == null || chargers.isEmpty()) return Result.success(true);
        boolean allOk = true;
        for (Charger c : chargers) {
            Charger patch = new Charger();
            patch.setChargerId(c.getChargerId());
            patch.setBillingPlanId(planId);
            boolean ok = chargerService.updateById(patch);
            if (!ok) allOk = false;
        }
        return Result.success(allOk);
    }


    @PostMapping("/chargers")
    @Operation(summary = "批量为充电桩设置计费计划")
    @DataScope
    public Result<Boolean> assignPlanBatch(@RequestParam Long planId, @RequestBody java.util.List<Long> chargerIds) {
        if (chargerIds == null || chargerIds.isEmpty()) return Result.success(true);
        boolean allOk = true;
        for (Long id : chargerIds) {
            Charger patch = new Charger();
            patch.setChargerId(id);
            patch.setBillingPlanId(planId);
            boolean ok = chargerService.updateById(patch);
            if (!ok) allOk = false;
        }
        return Result.success(allOk);
    }

}
