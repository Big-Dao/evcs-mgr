package com.evcs.station.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.station.entity.FirmwareUpgradeTask;

public interface FirmwareUpgradeTaskService extends IService<FirmwareUpgradeTask> {

    /**
     * 创建并开始升级任务
     * @param firmwareId 固件ID
     * @param chargerId 充电桩ID
     * @return 任务ID
     */
    Long createAndStartTask(Long firmwareId, Long chargerId);

    /**
     * 重试任务
     * @param taskId 任务ID
     */
    void retryTask(Long taskId);
}
