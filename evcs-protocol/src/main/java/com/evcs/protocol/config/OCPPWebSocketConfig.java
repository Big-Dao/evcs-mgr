package com.evcs.protocol.config;

import com.evcs.protocol.websocket.OCPPWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * OCPP WebSocket配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocket
@ConditionalOnProperty(name = "evcs.protocol.ocpp.enabled", havingValue = "true", matchIfMissing = true)
public class OCPPWebSocketConfig implements WebSocketConfigurer {

    private final OCPPWebSocketHandler ocppWebSocketHandler;
    private final ProtocolProperties protocolProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        int port = protocolProperties.getOcpp().getPort();
        String path = "/ocpp/{chargerCode}/websocket";

        // 注册WebSocket处理器
        registry.addHandler(ocppWebSocketHandler, path)
                .setAllowedOrigins("*") // 生产环境中应该限制允许的源
                .withSockJS(); // 启用SockJS支持

        log.info("OCPP WebSocket configured on port {} with path: {}", port, path);
        log.info("OCPP WebSocket URL: ws://localhost:{}/ocpp/CHARGER_001/websocket", port);
    }
}