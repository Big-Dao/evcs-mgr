package com.evcs.station.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.result.Result;
import com.evcs.station.entity.ChargingProfile;
import com.evcs.station.service.ChargingProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "智能充电", description = "充电策略配置与下发")
@RestController
@RequestMapping("/smart-charging")
@RequiredArgsConstructor
public class SmartChargingController {

    private final ChargingProfileService chargingProfileService;

    @Operation(summary = "创建充电策略", description = "创建新的充电策略配置")
    @PostMapping("/profile")
    public Result<Long> createProfile(@RequestBody ChargingProfile profile) {
        chargingProfileService.save(profile);
        return Result.success(profile.getId());
    }

    @Operation(summary = "下发充电策略", description = "将策略下发到充电桩")
    @PostMapping("/profile/{id}/apply")
    public Result<Void> applyProfile(@PathVariable Long id) {
        try {
            chargingProfileService.applyProfile(id);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "分页查询策略", description = "查询充电策略列表")
    @GetMapping("/profile/list")
    public Result<Page<ChargingProfile>> listProfiles(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size) {
        Page<ChargingProfile> pageParam = new Page<>(page, size);
        return Result.success(chargingProfileService.page(pageParam));
    }
}
