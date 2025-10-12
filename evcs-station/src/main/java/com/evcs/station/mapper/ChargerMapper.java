package com.evcs.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.station.entity.Charger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电桩数据访问接口
 */
@Mapper
public interface ChargerMapper extends BaseMapper<Charger> {

    /**
     * 根据充电站ID查询充电桩列表
     */
    @Select("""
        SELECT * FROM charger 
        WHERE station_id = #{stationId} AND deleted = 0
        ORDER BY charger_code
        """)
    List<Charger> selectByStationId(@Param("stationId") Long stationId);

    /**
     * 更新充电桩状态
     */
    @Update("""
        UPDATE charger 
        SET status = #{status}, 
            last_heartbeat = #{heartbeat},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
        """)
    int updateStatus(@Param("chargerId") Long chargerId, 
                    @Param("status") Integer status,
                    @Param("heartbeat") LocalDateTime heartbeat);

    /**
     * 更新充电桩实时数据
     */
    @Update("""
        UPDATE charger 
        SET current_power = #{power},
            current_voltage = #{voltage}, 
            current_current = #{current},
            temperature = #{temperature},
            last_heartbeat = #{heartbeat},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
        """)
    int updateRealTimeData(@Param("chargerId") Long chargerId,
                          @Param("power") Double power,
                          @Param("voltage") Double voltage, 
                          @Param("current") Double current,
                          @Param("temperature") Double temperature,
                          @Param("heartbeat") LocalDateTime heartbeat);

    /**
     * 开始充电会话
     */
    @Update("""
        UPDATE charger 
        SET status = 2,
            current_session_id = #{sessionId},
            current_user_id = #{userId},
            charging_start_time = #{startTime},
            charged_energy = 0,
            charged_duration = 0,
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
        """)
    int startChargingSession(@Param("chargerId") Long chargerId,
                           @Param("sessionId") String sessionId,
                           @Param("userId") Long userId,
                           @Param("startTime") LocalDateTime startTime);

    /**
     * 结束充电会话
     */
    @Update("""
        UPDATE charger 
        SET status = 1,
            current_session_id = NULL,
            current_user_id = NULL,
            charging_start_time = NULL,
            total_charging_sessions = total_charging_sessions + 1,
            total_charging_energy = total_charging_energy + #{energy},
            total_charging_time = total_charging_time + #{duration},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
        """)
    int endChargingSession(@Param("chargerId") Long chargerId,
                         @Param("energy") Double energy,
                         @Param("duration") Long duration);

    /**
     * 更新充电进度
     */
    @Update("""
        UPDATE charger 
        SET charged_energy = #{energy},
            charged_duration = #{duration},
            last_heartbeat = #{heartbeat},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
        """)
    int updateChargingProgress(@Param("chargerId") Long chargerId,
                             @Param("energy") Double energy,
                             @Param("duration") Integer duration,
                             @Param("heartbeat") LocalDateTime heartbeat);

    /**
     * 查询离线充电桩
     */
    @Select("""
        SELECT * FROM charger 
        WHERE status != 0 
        AND last_heartbeat < #{threshold}
        AND deleted = 0
        """)
    List<Charger> selectOfflineChargers(@Param("threshold") LocalDateTime threshold);

    /**
     * 查询故障充电桩
     */
    @Select("""
        SELECT * FROM charger 
        WHERE status = 3 
        AND deleted = 0
        ORDER BY update_time DESC
        """)
    List<Charger> selectFaultChargers();

    /**
     * 统计充电桩状态
     */
    @Select("""
        SELECT status, COUNT(*) as count
        FROM charger 
        WHERE tenant_id = #{tenantId} AND deleted = 0
        GROUP BY status
        """)
    List<java.util.Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId);

    /**
     * 根据协议类型查询充电桩
     */
    @Select("""
        SELECT * FROM charger 
        WHERE supported_protocols::text LIKE CONCAT('%"', #{protocol}, '"%')
        AND deleted = 0
        """)
    List<Charger> selectByProtocol(@Param("protocol") String protocol);
}