package com.evcs.station.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.station.entity.Station;
import com.evcs.station.mapper.StationMapper;
import com.evcs.station.service.IStationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电站服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationServiceImpl extends ServiceImpl<StationMapper, Station> implements IStationService {

    /**
     * 分页查询充电站列表（包含统计信息）
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public IPage<Station> queryStationPage(Page<Station> page, Station queryParam) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        
        // 根据充电站名称查询
        if (StrUtil.isNotBlank(queryParam.getStationName())) {
            wrapper.like("station_name", queryParam.getStationName());
        }
        
        // 根据充电站编码查询
        if (StrUtil.isNotBlank(queryParam.getStationCode())) {
            wrapper.like("station_code", queryParam.getStationCode());
        }
        
        // 根据状态查询
        if (queryParam.getStatus() != null) {
            wrapper.eq("status", queryParam.getStatus());
        }
        
        // 根据省市区查询
        if (StrUtil.isNotBlank(queryParam.getProvince())) {
            wrapper.eq("province", queryParam.getProvince());
        }
        if (StrUtil.isNotBlank(queryParam.getCity())) {
            wrapper.eq("city", queryParam.getCity());
        }
        if (StrUtil.isNotBlank(queryParam.getDistrict())) {
            wrapper.eq("district", queryParam.getDistrict());
        }
        
        // 只查询未删除
        wrapper.eq("deleted", 0);
        // 排序
        wrapper.orderByDesc("create_time");
        
        return baseMapper.selectStationPageWithStats(page, wrapper);
    }

    /**
     * 根据ID查询充电站详情（包含统计信息）
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public Station getStationDetail(Long stationId) {
        return baseMapper.selectStationWithStats(stationId);
    }

    /**
     * 新增充电站
     * 自动设置租户ID和创建信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveStation(Station station) {
        // 检查充电站编码是否重复
        if (checkStationCodeExists(station.getStationCode(), null)) {
            throw new RuntimeException("充电站编码已存在");
        }
        
        // 设置租户信息
        station.setTenantId(TenantContext.getCurrentTenantId());
        station.setCreateTime(LocalDateTime.now());
        station.setCreateBy(TenantContext.getCurrentUserId());
        
        // 设置默认值
        if (station.getStatus() == null) {
            station.setStatus(1); // 默认启用
        }
        if (station.getTotalChargers() == null) {
            station.setTotalChargers(0);
        }
        if (station.getAvailableChargers() == null) {
            station.setAvailableChargers(0);
        }
        
        return this.save(station);
    }

    /**
     * 更新充电站信息
     * 数据权限：只能更新有权限的充电站
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean updateStation(Station station) {
        // 检查充电站是否存在
        Station existStation = this.getById(station.getStationId());
        if (existStation == null) {
            throw new RuntimeException("充电站不存在");
        }
        
        // 检查编码是否重复（排除自己）
        if (StrUtil.isNotBlank(station.getStationCode()) && 
            checkStationCodeExists(station.getStationCode(), station.getStationId())) {
            throw new RuntimeException("充电站编码已存在");
        }
        
        // 设置更新信息
        station.setUpdateTime(LocalDateTime.now());
        station.setUpdateBy(TenantContext.getCurrentUserId());
        
        // 不允许修改租户ID
        station.setTenantId(null);
        
        return this.updateById(station);
    }

    /**
     * 删除充电站（逻辑删除）
     * 数据权限：只能删除有权限的充电站
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean deleteStation(Long stationId) {
        // 检查是否有充电桩
        // TODO: 实现检查逻辑
        
        return this.removeById(stationId);
    }

    /**
     * 查询附近充电站
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public List<Station> getNearbyStations(Double latitude, Double longitude, Double radius, Integer limit) {
        if (radius == null) {
            radius = 10.0; // 默认10公里
        }
        if (limit == null) {
            limit = 20; // 默认返回20个
        }
        
        return baseMapper.selectNearbyStations(latitude, longitude, radius, limit);
    }

    /**
     * 启用/停用充电站
     * 数据权限：只能操作有权限的充电站
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean changeStatus(Long stationId, Integer status) {
        Station station = new Station();
        station.setStationId(stationId);
        station.setStatus(status);
        station.setUpdateTime(LocalDateTime.now());
        station.setUpdateBy(TenantContext.getCurrentUserId());
        
        return this.updateById(station);
    }

    /**
     * 检查充电站编码是否存在
     */
    @Override
    public boolean checkStationCodeExists(String stationCode, Long excludeId) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        wrapper.eq("station_code", stationCode);
        wrapper.eq("tenant_id", TenantContext.getCurrentTenantId());
        
        if (excludeId != null) {
            wrapper.ne("station_id", excludeId);
        }
        
        return this.count(wrapper) > 0;
    }

    /**
     * 统计租户下的充电站数量
     */
    @Override
    @DataScope
    public Long countByTenantId(Long tenantId) {
        return baseMapper.countByTenantId(tenantId);
    }

    /**
     * 导入充电站数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importStations(List<Station> stations) {
        Long tenantId = TenantContext.getCurrentTenantId();
        Long userId = TenantContext.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        
        for (Station station : stations) {
            // 检查编码是否重复
            if (checkStationCodeExists(station.getStationCode(), null)) {
                log.warn("充电站编码已存在，跳过导入: {}", station.getStationCode());
                continue;
            }
            
            // 设置租户和审计信息
            station.setTenantId(tenantId);
            station.setCreateTime(now);
            station.setCreateBy(userId);
            
            // 设置默认值
            if (station.getStatus() == null) {
                station.setStatus(1);
            }
        }
        
        return this.saveBatch(stations);
    }

    /**
     * 导出充电站数据
     */
    @Override
    @DataScope
    public List<Station> exportStations(Station queryParam) {
        QueryWrapper<Station> wrapper = new QueryWrapper<>();
        
        // 根据查询条件构建wrapper
        if (queryParam != null) {
            if (StrUtil.isNotBlank(queryParam.getStationName())) {
                wrapper.like("station_name", queryParam.getStationName());
            }
            if (queryParam.getStatus() != null) {
                wrapper.eq("status", queryParam.getStatus());
            }
            if (StrUtil.isNotBlank(queryParam.getProvince())) {
                wrapper.eq("province", queryParam.getProvince());
            }
        }
        
        wrapper.orderByDesc("create_time");
        
        return this.list(wrapper);
    }
}