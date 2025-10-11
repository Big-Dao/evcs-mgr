package com.evcs.protocol.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 开始充电事件
 * 充电开始时触发
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StartEvent extends ProtocolEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 初始电量（kWh）
     */
    private Double initialEnergy;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 响应消息
     */
    private String message;

    @Override
    public String getRoutingKey() {
        return "protocol.charging.start";
    }
}
