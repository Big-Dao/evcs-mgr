package com.evcs.protocol.event;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 实时遥测/计量事件（用于实时展示与会话曲线）
 *
 * 典型来源：OCPP 1.6 MeterValues
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TelemetryEvent extends ProtocolEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID（例如：OCPP_TXN_<transactionId>）
     */
    private String sessionId;

    /**
     * OCPP transactionId（可选，用于调试/关联）
     */
    private Integer transactionId;

    /**
     * 采样时间（来自协议 payload 的 timestamp）
     */
    private LocalDateTime sampleTime;

    /**
     * 电压（V）
     */
    private Double voltage;

    /**
     * 电流（A）
     */
    private Double current;

    /**
     * 功率（kW）
     */
    private Double power;

    /**
     * SOC（%）
     */
    private Double soc;

    /**
     * 累计电量（kWh）
     */
    private Double energy;

    /**
     * 会话持续时长（秒，可选；若无法计算则为空）
     */
    private Long durationSeconds;

    @Override
    public String getRoutingKey() {
        return "protocol.telemetry." + getProtocolType();
    }
}
