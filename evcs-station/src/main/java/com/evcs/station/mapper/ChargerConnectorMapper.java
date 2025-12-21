package com.evcs.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
}
