package com.evcs.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.station.entity.ChargerConnector;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChargerConnectorMapper extends BaseMapper<ChargerConnector> {

    @Select("""
        <script>
        SELECT cc.*, c.charger_code
        FROM charger_connector cc
        LEFT JOIN charger c ON cc.charger_id = c.charger_id
        WHERE cc.deleted = 0
        <if test="param.connectorNo != null">
            AND cc.connector_no = #{param.connectorNo}
        </if>
        <if test="param.chargerCode != null and param.chargerCode != ''">
            AND c.charger_code LIKE CONCAT('%', #{param.chargerCode}, '%')
        </if>
        <if test="param.status != null">
            AND cc.status = #{param.status}
        </if>
        ORDER BY cc.charger_id, cc.connector_no
        </script>
        """)
    IPage<ChargerConnector> selectPageList(Page<?> page, @Param("param") ChargerConnector param);

    @Select("""
        SELECT * FROM charger_connector
        WHERE charger_id = #{chargerId} AND deleted = 0
        ORDER BY connector_no
        """)
    java.util.List<ChargerConnector> selectByChargerId(@Param("chargerId") Long chargerId);

    @Select("""
        SELECT * FROM charger_connector
        WHERE charger_id = #{chargerId} AND connector_no = #{connectorNo} AND deleted = 0
        LIMIT 1
        """)
    ChargerConnector selectByChargerIdAndConnectorNo(
        @Param("chargerId") Long chargerId,
        @Param("connectorNo") Integer connectorNo
    );

    @Update("""
        UPDATE charger_connector
        SET status = #{status},
            fault_code = #{faultCode},
            fault_description = #{faultDescription},
            last_heartbeat = COALESCE(#{heartbeat}, last_heartbeat),
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
          AND connector_no = #{connectorNo}
          AND deleted = 0
        """)
    int updateStatus(
        @Param("chargerId") Long chargerId,
        @Param("connectorNo") Integer connectorNo,
        @Param("status") Integer status,
        @Param("faultCode") String faultCode,
        @Param("faultDescription") String faultDescription,
        @Param("heartbeat") LocalDateTime heartbeat
    );

    @Update("""
        UPDATE charger_connector
        SET last_heartbeat = #{heartbeat},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
          AND deleted = 0
        """)
    int touchAllHeartbeat(
        @Param("chargerId") Long chargerId,
        @Param("heartbeat") LocalDateTime heartbeat
    );

    @Update("""
        UPDATE charger_connector
        SET current_session_id = #{sessionId},
            current_user_id = #{userId},
            charging_start_time = #{startTime},
            charged_energy = #{energy},
            charged_duration = #{duration},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
          AND connector_no = #{connectorNo}
          AND deleted = 0
        """)
    int updateSessionStart(
        @Param("chargerId") Long chargerId,
        @Param("connectorNo") Integer connectorNo,
        @Param("sessionId") String sessionId,
        @Param("userId") Long userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("energy") BigDecimal energy,
        @Param("duration") Integer duration
    );

    @Update("""
        UPDATE charger_connector
        SET current_session_id = NULL,
            current_user_id = NULL,
            charging_start_time = NULL,
            charged_energy = #{energy},
            charged_duration = #{duration},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
          AND connector_no = #{connectorNo}
          AND current_session_id = #{sessionId}
          AND deleted = 0
        """)
    int updateSessionStopBySessionId(
        @Param("chargerId") Long chargerId,
        @Param("connectorNo") Integer connectorNo,
        @Param("sessionId") String sessionId,
        @Param("energy") BigDecimal energy,
        @Param("duration") Integer duration
    );

    @Update("""
        UPDATE charger_connector
        SET current_session_id = NULL,
            current_user_id = NULL,
            charging_start_time = NULL,
            charged_energy = #{energy},
            charged_duration = #{duration},
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
          AND connector_no = #{connectorNo}
          AND deleted = 0
        """)
    int updateSessionStopFallback(
        @Param("chargerId") Long chargerId,
        @Param("connectorNo") Integer connectorNo,
        @Param("energy") BigDecimal energy,
        @Param("duration") Integer duration
    );

    @Update("""
        UPDATE charger_connector
        SET last_meter_time = COALESCE(#{sampleTime}, last_meter_time),
            last_voltage = COALESCE(#{voltage}, last_voltage),
            last_current = COALESCE(#{current}, last_current),
            last_power = COALESCE(#{power}, last_power),
            last_soc = COALESCE(#{soc}, last_soc),
            last_energy = COALESCE(#{energy}, last_energy),
            update_time = CURRENT_TIMESTAMP
        WHERE charger_id = #{chargerId}
          AND connector_no = #{connectorNo}
          AND deleted = 0
          AND (#{sessionId} IS NULL OR current_session_id = #{sessionId})
        """)
    int updateTelemetrySnapshot(
        @Param("chargerId") Long chargerId,
        @Param("connectorNo") Integer connectorNo,
        @Param("sessionId") String sessionId,
        @Param("sampleTime") LocalDateTime sampleTime,
        @Param("voltage") BigDecimal voltage,
        @Param("current") BigDecimal current,
        @Param("power") BigDecimal power,
        @Param("soc") BigDecimal soc,
        @Param("energy") BigDecimal energy
    );
}
