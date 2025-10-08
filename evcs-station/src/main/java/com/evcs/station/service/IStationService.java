package com.evcs.station.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.station.entity.Station;

import java.util.List;

/**
 * 充电站服务接口
 */
public interface IStationService extends IService<Station> {

    /**
     * 分页查询充电站列表（包含统计信息）
     */
    IPage<Station> queryStationPage(Page<Station> page, Station queryParam);

    /**
     * 根据ID查询充电站详情（包含统计信息）
     */
    Station getStationDetail(Long stationId);

    /**
     * 新增充电站
     */
    boolean saveStation(Station station);

    /**
     * 更新充电站信息
     */
    boolean updateStation(Station station);

    /**
     * 删除充电站（逻辑删除）
     */
    boolean deleteStation(Long stationId);

    /**
     * 查询附近充电站
     */
    List<Station> getNearbyStations(Double latitude, Double longitude, Double radius, Integer limit);

    /**
     * 启用/停用充电站
     */
    boolean changeStatus(Long stationId, Integer status);

    /**
     * 检查充电站编码是否存在
     */
    boolean checkStationCodeExists(String stationCode, Long excludeId);

    /**
     * 统计租户下的充电站数量
     */
    Long countByTenantId(Long tenantId);

    /**
     * 导入充电站数据
     */
    boolean importStations(List<Station> stations);

    /**
     * 导出充电站数据
     */
    List<Station> exportStations(Station queryParam);
}