package com.evcs.station.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.result.Result;
import com.evcs.station.entity.Firmware;
import com.evcs.station.dto.FirmwareTaskResponse;
import com.evcs.station.entity.FirmwareUpgradeTask;
import com.evcs.station.service.FirmwareService;
import com.evcs.station.service.FirmwareUpgradeTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "固件管理", description = "固件上传、版本管理及升级任务")
@RestController
@RequestMapping("/firmware")
@RequiredArgsConstructor
public class FirmwareController {

    private final FirmwareService firmwareService;
    private final FirmwareUpgradeTaskService firmwareUpgradeTaskService;

    @Operation(summary = "上传固件信息", description = "登记新固件版本信息")
    @PostMapping("/upload")
    public Result<Long> uploadFirmware(@RequestBody Firmware firmware) {
        firmwareService.save(firmware);
        return Result.success(firmware.getId());
    }

    @Operation(summary = "分页查询固件", description = "查询固件列表")
    @GetMapping("/list")
    public Result<Page<Firmware>> listFirmware(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        Page<Firmware> pageParam = new Page<>(page, size);
        return Result.success(firmwareService.page(pageParam));
    }

    @Operation(summary = "创建升级任务", description = "对指定充电桩发起固件升级")
    @PostMapping("/upgrade")
    public Result<Long> createUpgradeTask(@RequestParam Long firmwareId, @RequestParam Long chargerId) {
        try {
            Long taskId = firmwareUpgradeTaskService.createAndStartTask(firmwareId, chargerId);
            return Result.success(taskId);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "重试升级任务", description = "对失败的任务进行重试")
    @PostMapping("/retry")
    public Result<Void> retryUpgradeTask(@RequestParam Long taskId) {
        try {
            firmwareUpgradeTaskService.retryTask(taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @Operation(summary = "查询升级任务", description = "查询升级任务列表")
    @GetMapping("/tasks")
    public Result<List<FirmwareTaskResponse>> listTasks(@RequestParam(required = false) Long chargerId) {
        LambdaQueryWrapper<FirmwareUpgradeTask> wrapper = new LambdaQueryWrapper<>();
        if (chargerId != null) {
            wrapper.eq(FirmwareUpgradeTask::getChargerId, chargerId);
        }
        wrapper.orderByDesc(FirmwareUpgradeTask::getCreateTime);
        return Result.success(firmwareUpgradeTaskService.list(wrapper).stream()
                .map(FirmwareTaskResponse::from).toList());
    }
}
