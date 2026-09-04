package com.evcs.protocol.mq;

/**
 * 协议事件流 MQ 拓扑常量（交换机/队列/路由键）。
 *
 * <p>拓扑 Bean 由协议服务声明；消费方（order/station）仅引用名称，
 * 通过本常量类共享，避免各自硬编码字符串漂移。
 */
public final class ProtocolMqConstants {

    private ProtocolMqConstants() {
    }

    /** 协议事件交换机 */
    public static final String PROTOCOL_EXCHANGE = "evcs.protocol.events";

    public static final String HEARTBEAT_QUEUE = "evcs.protocol.heartbeat";
    public static final String STATUS_QUEUE = "evcs.protocol.status";
    public static final String CHARGING_QUEUE = "evcs.protocol.charging";
    public static final String TELEMETRY_QUEUE = "evcs.protocol.telemetry";

    /** 死信交换机与队列 */
    public static final String DLX_EXCHANGE = "evcs.protocol.dlx";
    public static final String DLX_QUEUE = "evcs.protocol.dlx.queue";

    public static final String HEARTBEAT_ROUTING_KEY = "protocol.heartbeat.*";
    public static final String STATUS_ROUTING_KEY = "protocol.status.*";
    public static final String TELEMETRY_ROUTING_KEY = "protocol.telemetry.*";
    public static final String CHARGING_START_ROUTING_KEY = "protocol.charging.start";
    public static final String CHARGING_STOP_ROUTING_KEY = "protocol.charging.stop";
}
