package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.station.entity.Charger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 充电桩服务接口
 */
public interface IChargerService extends IService<Charger> {

    /**
     * 分页查询充电桩列表
     */
    IPage<Charger> queryChargerPage(Page<Charger> page, Charger queryParam);

    /**
     * 根据充电站ID查询充电桩列表
     */
    List<Charger> getChargersByStationId(Long stationId);

    /**
     * 新增充电桩
     */
    boolean saveCharger(Charger charger);

    /**
     * 更新充电桩信息
     */
    boolean updateCharger(Charger charger);

    /**
     * 删除充电桩（逻辑删除）
     */
    boolean deleteCharger(Long chargerId);

    /**
     * 更新充电桩状态
     */
    boolean updateStatus(Long chargerId, Integer status);

    /**
     * 更新充电桩实时数据
     */
    boolean updateRealTimeData(Long chargerId, Double power, Double voltage, Double current, Double temperature);

    /**
     * 开始充电会话
     */
    boolean startChargingSession(Long chargerId, String sessionId, Long userId);

    /**
     * 结束充电会话
     */
    boolean endChargingSession(Long chargerId, Double energy, Long duration);

    /**
     * 更新充电进度
     */
    boolean updateChargingProgress(Long chargerId, Double energy, Integer duration);

    /**
     * 检查充电桩编码是否存在
     */
    boolean checkChargerCodeExists(String chargerCode, Long excludeId);

    /**
     * 查询离线充电桩
     */
    List<Charger> getOfflineChargers(Integer minutes);

    /**
     * 查询故障充电桩
     */
    List<Charger> getFaultChargers();

    /**
     * 统计充电桩状态
     */
    Map<Integer, Long> getStatusStatistics(Long tenantId);

    /**
     * 根据协议类型查询充电桩
     */
    List<Charger> getChargersByProtocol(String protocol);

    /**
     * 批量更新充电桩状态
     */
    boolean batchUpdateStatus(List<Long> chargerIds, Integer status);

    /**
     * 启用/停用充电桩
     */
    boolean changeStatus(Long chargerId, Integer enabled);

    /**
     * 重置充电桩（清除当前会话）
     */
    boolean resetCharger(Long chargerId);
}