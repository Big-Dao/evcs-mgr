package com.evcs.protocol.api;

import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;

/**
 * 统一协议服务接口
 */
public interface IProtocolService {

    /**
     * 连接设备
     *
     * @param deviceCode 设备编码
     * @param protocolType 协议类型
     * @return 连接结果
     */
    boolean connect(String deviceCode, ProtocolType protocolType);

    /**
     * 断开连接
     *
     * @param deviceCode 设备编码
     * @param protocolType 协议类型
     */
    void disconnect(String deviceCode, ProtocolType protocolType);

    /**
     * 发送心跳
     *
     * @param request 心跳请求
     * @return 心跳响应
     */
    ProtocolResponse sendHeartbeat(ProtocolRequest request);

    /**
     * 更新状态
     *
     * @param request 状态更新请求
     * @return 状态更新响应
     */
    ProtocolResponse updateStatus(ProtocolRequest request);

    /**
     * 开始充电
     *
     * @param request 开始充电请求
     * @return 开始充电响应
     */
    ProtocolResponse startCharging(ProtocolRequest request);

    /**
     * 停止充电
     *
     * @param request 停止充电请求
     * @return 停止充电响应
     */
    ProtocolResponse stopCharging(ProtocolRequest request);

    /**
     * 注册站点（云快充协议专用）
     *
     * @param request 注册请求
     * @return 注册响应
     */
    ProtocolResponse registerStation(ProtocolRequest request);

    /**
     * 处理通用协议请求
     *
     * @param request 协议请求
     * @return 协议响应
     */
    ProtocolResponse handleRequest(ProtocolRequest request);

    /**
     * 检查连接状态
     *
     * @param deviceCode 设备编码
     * @param protocolType 协议类型
     * @return 是否连接
     */
    boolean isConnected(String deviceCode, ProtocolType protocolType);

    /**
     * 设置协议事件监听器
     *
     * @param listener 事件监听器
     */
    void setEventListener(ProtocolEventListener listener);

    /**
     * 获取支持的协议类型
     *
     * @return 支持的协议类型
     */
    ProtocolType getSupportedProtocolType();
}