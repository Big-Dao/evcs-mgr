package com.evcs.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.station.entity.Station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 充电站数据访问接口
 */
@Mapper
public interface StationMapper extends BaseMapper<Station> {

    /**
     * 分页查询充电站列表（包含统计信息）
     */
    @Select("""
        SELECT s.station_id, s.tenant_id, s.station_code, s.station_name, s.address,
               s.latitude, s.longitude, s.status, s.province, s.city, s.district,
               s.create_time, s.update_time, s.create_by, s.update_by, s.deleted,
               COALESCE(c.total_chargers, 0) as total_chargers,
               COALESCE(c.available_chargers, 0) as available_chargers,
               COALESCE(c.charging_chargers, 0) as charging_chargers,
               COALESCE(c.fault_chargers, 0) as fault_chargers
        FROM charging_station s
        LEFT JOIN (
            SELECT station_id,
                   COUNT(*) as total_chargers,
                   COUNT(CASE WHEN status = 1 THEN 1 END) as available_chargers,
                   COUNT(CASE WHEN status = 2 THEN 1 END) as charging_chargers,
                   COUNT(CASE WHEN status = 3 THEN 1 END) as fault_chargers
            FROM charger 
            WHERE deleted = 0
            GROUP BY station_id
        ) c ON s.station_id = c.station_id
        ${ew.customSqlSegment}
        """ )
    IPage<Station> selectStationPageWithStats(Page<Station> page, @Param("ew") com.baomidou.mybatisplus.core.conditions.Wrapper<Station> wrapper);

    /**
     * 根据位置查询附近充电站
     */
    @Select("""
        SELECT s.station_id, s.tenant_id, s.station_code, s.station_name, s.address,
               s.latitude, s.longitude, s.status, s.province, s.city, s.district,
               COALESCE(stats.total_chargers, 0) as total_chargers,
               COALESCE(stats.available_chargers, 0) as available_chargers,
               COALESCE(stats.charging_chargers, 0) as charging_chargers,
               COALESCE(stats.fault_chargers, 0) as fault_chargers,
               s.create_time, s.update_time, s.create_by, s.update_by, s.deleted,
               (6371 * acos(cos(radians(#{latitude})) * cos(radians(s.latitude)) * 
                cos(radians(s.longitude) - radians(#{longitude})) + 
                sin(radians(#{latitude})) * sin(radians(s.latitude)))) AS distance
        FROM charging_station s
        LEFT JOIN (
            SELECT station_id,
                   COUNT(*) as total_chargers,
                   SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as available_chargers,
                   SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as charging_chargers,
                   SUM(CASE WHEN status = 3 OR status = 0 THEN 1 ELSE 0 END) as fault_chargers
            FROM charger
            WHERE deleted = 0
            GROUP BY station_id
        ) stats ON s.station_id = stats.station_id
        WHERE s.deleted = 0 AND s.status = 1
          AND (6371 * acos(cos(radians(#{latitude})) * cos(radians(s.latitude)) * 
                cos(radians(s.longitude) - radians(#{longitude})) + 
                sin(radians(#{latitude})) * sin(radians(s.latitude)))) <= #{radius}
        ORDER BY distance
        LIMIT #{limit}
        """)
    List<Station> selectNearbyStations(@Param("latitude") Double latitude, 
                                     @Param("longitude") Double longitude,
                                     @Param("radius") Double radius,
                                     @Param("limit") Integer limit);

    /**
     * 统计租户下的充电站数量
     */
    @Select("SELECT COUNT(*) FROM charging_station WHERE tenant_id = #{tenantId} AND deleted = 0")
    Long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询充电站详情（包含充电桩信息）
     */
    @Select("""
        SELECT s.station_id, s.tenant_id, s.station_code, s.station_name, s.address,
               s.latitude, s.longitude, s.status, s.province, s.city, s.district,
               s.create_time, s.update_time, s.create_by, s.update_by, s.deleted,
               COALESCE(stats.total_chargers, 0) as total_chargers,
               COALESCE(stats.available_chargers, 0) as available_chargers,
               COALESCE(stats.charging_chargers, 0) as charging_chargers,
               COALESCE(stats.fault_chargers, 0) as fault_chargers,
               COALESCE(stats.total_power, 0) as totalPower
        FROM charging_station s
        LEFT JOIN (
            SELECT station_id,
                   COUNT(*) as total_chargers,
                   COUNT(CASE WHEN status = 1 THEN 1 END) as available_chargers,
                   COUNT(CASE WHEN status = 2 THEN 1 END) as charging_chargers,
                   COUNT(CASE WHEN status = 3 THEN 1 END) as fault_chargers,
                   SUM(rated_power) as total_power
            FROM charger 
            WHERE deleted = 0
            GROUP BY station_id
        ) stats ON s.station_id = stats.station_id
        WHERE s.station_id = #{stationId} AND s.deleted = 0
        """)
    Station selectStationWithStats(@Param("stationId") Long stationId);
}