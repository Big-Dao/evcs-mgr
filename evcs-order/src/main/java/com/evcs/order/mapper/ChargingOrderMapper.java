package com.evcs.order.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.evcs.order.dto.CityOrderStatistics;
import com.evcs.order.dto.OrderDTO;
import com.evcs.order.entity.ChargingOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChargingOrderMapper extends BaseMapper<ChargingOrder> {

    /**
     * 分页查询订单列表（包含关联信息）
     *
     * @param page 分页对象
     * @param wrapper 查询条件
     * @return 订单DTO分页对象
     */
    @Select("SELECT co.*, " +
            "cs.station_name as station_name, " +
            "c.charger_code as charger_code, " +
            "co.session_id as order_no, " +
            "co.energy as charging_amount, " +
            "co.amount as total_amount " +
            "FROM charging_order co " +
            "LEFT JOIN charging_station cs ON co.station_id = cs.station_id " +
            "LEFT JOIN charger c ON co.charger_id = c.charger_id " +
            "${ew.customSqlSegment}")
    IPage<OrderDTO> selectOrderListCustom(IPage<OrderDTO> page, @Param(Constants.WRAPPER) Wrapper<ChargingOrder> wrapper);
    
    /**
     * 按城市统计订单数据
     * 用于地图可视化分析
     * 
     * @param tenantId 租户ID（多租户隔离）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 城市订单统计列表
     */
    @Select({
        "<script>",
        "SELECT ",
        "  s.province,",
        "  s.city,",
        "  COUNT(o.id) as orderCount,",
        "  COUNT(DISTINCT o.station_id) as stationCount,",
        "  COALESCE(SUM(o.energy), 0) as totalEnergy,",
        "  COALESCE(SUM(o.amount), 0) as totalAmount",
        " FROM charging_order o",
        " INNER JOIN charging_station s ON o.station_id = s.station_id",
        " WHERE o.tenant_id = #{tenantId}",
        " AND o.deleted = 0",
        " AND s.deleted = 0",
        " <if test='startTime != null'>",
        "   AND o.start_time &gt;= #{startTime}",
        " </if>",
        " <if test='endTime != null'>",
        "   AND o.start_time &lt;= #{endTime}",
        " </if>",
        " GROUP BY s.province, s.city",
        " ORDER BY orderCount DESC",
        "</script>"
    })
    List<CityOrderStatistics> getCityOrderStatistics(
        @Param("tenantId") Long tenantId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
