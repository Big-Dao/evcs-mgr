package com.evcs.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payment.alipay")
public class AlipayConfig {

    /**
     * 支付宝应用ID
     */
    private String appId;

    /**
     * 商户私钥（PKCS8格式）
     */
    private String merchantPrivateKey;

    /**
     * 支付宝公钥（用于验证回调签名）
     */
    private String alipayPublicKey;

    /**
     * 支付回调地址
     */
    private String notifyUrl;

    /**
     * 支付完成跳转地址
     */
    private String returnUrl;

    /**
     * 签名算法类型
     */
    private String signType = "RSA2";

    /**
     * 字符编码
     */
    private String charset = "UTF-8";

    /**
     * 支付宝网关地址
     */
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 格式类型
     */
    private String format = "json";

    /**
     * 是否沙箱环境
     */
    private boolean sandbox = true;

    /**
     * 应用公钥证书路径（证书模式使用）
     */
    private String appCertPath;

    /**
     * 支付宝公钥证书路径（证书模式使用）
     */
    private String alipayCertPath;

    /**
     * 支付宝根证书路径（证书模式使用）
     */
    private String alipayRootCertPath;

    /**
     * 是否使用证书模式
     */
    private boolean useCertMode = false;
}