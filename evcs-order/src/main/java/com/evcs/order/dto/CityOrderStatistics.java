package com.evcs.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 城市订单统计DTO
 * 用于地图分析展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityOrderStatistics {
    /**
     * 省份名称
     */
    private String province;

    /**
     * 城市名称
     */
    private String city;

    /**
     * 订单数量
     */
    private Long orderCount;

    /**
     * 充电站数量
     */
    private Long stationCount;

    /**
     * 总充电量(kWh)
     */
    private Double totalEnergy;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;
}
