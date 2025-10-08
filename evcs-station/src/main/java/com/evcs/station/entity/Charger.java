package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.evcs.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 充电桩实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("charger")
public class Charger extends BaseEntity {
    /**
     * 主键ID
     */
    @TableId
    private Long chargerId;

    /**
     * 租户ID（多租户隔离字段）
     */
    private Long tenantId;
    
    /**
     * 充电桩编码
     */
    private String chargerCode;
    
    /**
     * 充电桩名称
     */
    private String chargerName;
    
    /**
     * 充电站ID
     */
    private Long stationId;
    
    /**
     * 充电站编码
     */
    private String stationCode;
    
    /**
     * 充电桩类型：1-直流快充，2-交流慢充
     */
    private Integer chargerType;
    
    /**
     * 充电桩品牌
     */
    private String brand;
    
    /**
     * 充电桩型号
     */
    private String model;
    
    /**
     * 生产厂商
     */
    private String manufacturer;
    
    /**
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 投入运营日期
     */
    private LocalDate operationDate;
    
    /**
     * 额定功率（kW）
     */
    private BigDecimal ratedPower;
    
    /**
     * 输入电压（V）
     */
    private Integer inputVoltage;
    
    /**
     * 输出电压范围（V）
     */
    private String outputVoltageRange;
    
    /**
     * 输出电流范围（A）
     */
    private String outputCurrentRange;
    
    /**
     * 枪头数量
     */
    private Integer gunCount;
    
    /**
     * 枪头类型：使用PostgreSQL数组存储
     * 例如：{1,2} 表示支持国标和欧标
     */
    private String gunTypes;
    
    /**
     * 支持的充电协议（JSONB格式）
     * 例如：{"ocpp": "1.6", "cloudCharge": "2.0"}
     */
    private String supportedProtocols;
    
    /**
     * 当前状态：0-离线，1-空闲，2-充电中，3-故障，4-维护，5-预约中
     */
    private Integer status;
    
    /**
     * 故障代码
     */
    private String faultCode;
    
    /**
     * 故障描述
     */
    private String faultDescription;
    
    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;
    
    /**
     * 累计充电次数
     */
    private Long totalChargingSessions;
    
    /**
     * 累计充电量（kWh）
     */
    private BigDecimal totalChargingEnergy;
    
    /**
     * 累计充电时长（分钟）
     */
    private Long totalChargingTime;
    
    /**
     * 当前充电会话ID
     */
    private String currentSessionId;
    
    /**
     * 当前用户ID
     */
    private Long currentUserId;
    
    /**
     * 充电开始时间
     */
    private LocalDateTime chargingStartTime;
    
    /**
     * 已充电量（kWh）
     */
    private BigDecimal chargedEnergy;
    
    /**
     * 已充电时长（分钟）
     */
    private Integer chargedDuration;
    
    /**
     * 实时功率（kW）
     */
    private BigDecimal currentPower;
    
    /**
     * 实时电压（V）
     */
    private BigDecimal currentVoltage;
    
    /**
     * 实时电流（A）
     */
    private BigDecimal currentCurrent;
    
    /**
     * 温度（℃）
     */
    private BigDecimal temperature;
    
    /**
     * 网络信号强度（dbm）
     */
    private Integer signalStrength;
    
    /**
     * 固件版本
     */
    private String firmwareVersion;
    
    /**
     * 最后维护时间
     */
    private LocalDateTime lastMaintenanceTime;

    /**
     * 下次维护时间
     */
    private LocalDateTime nextMaintenanceTime;
    
    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 计费计划ID（可选）。优先使用充电桩指定的计划，没有则使用站点默认计划
     */
    private Long billingPlanId;
    
    /**
     * 备注
     */
    private String remark;
}