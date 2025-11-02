package com.evcs.payment.service.channel;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.evcs.payment.config.AlipayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 支付宝客户端工厂
 */
@Slf4j
@Component
public class AlipayClientFactory {

    @Resource
    private AlipayConfig alipayConfig;

    /**
     * 获取支付宝客户端
     */
    public AlipayClient getAlipayClient() {
        try {
            // 根据环境选择网关
            String gatewayUrl = alipayConfig.isSandbox()
                ? "https://openapi.alipaydev.com/gateway.do"
                : alipayConfig.getGatewayUrl();

            log.info("创建支付宝客户端: gatewayUrl={}, appId={}, sandbox={}",
                gatewayUrl, alipayConfig.getAppId(), alipayConfig.isSandbox());

            DefaultAlipayClient client = new DefaultAlipayClient(
                gatewayUrl,
                alipayConfig.getAppId(),
                alipayConfig.getMerchantPrivateKey(),
                alipayConfig.getFormat(),
                alipayConfig.getCharset(),
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getSignType()
            );

            log.info("支付宝客户端创建成功");
            return client;

        } catch (Exception e) {
            log.error("创建支付宝客户端失败", e);
            throw new RuntimeException("创建支付宝客户端失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取证书模式支付宝客户端（暂未实现）
     */
    public AlipayClient getCertAlipayClient() {
        // 证书模式暂未实现，返回普通客户端
        log.warn("证书模式暂未实现，使用普通客户端");
        return getAlipayClient();
    }
}