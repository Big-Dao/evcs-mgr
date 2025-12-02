package com.evcs.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 支付配置
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "evcs.payment")
@Data
public class PaymentConfig {

    /**
     * 支付宝配置
     */
    private AlipayConfig alipay = new AlipayConfig();

    /**
     * 微信支付配置
     */
    private WechatConfig wechat = new WechatConfig();

    /**
     * 是否启用支付（测试环境可以禁用真实支付）
     */
    private boolean enabled = true;

    /**
     * 回调地址前缀
     */
    private String callbackUrlPrefix = "http://localhost:8084/api/payment/callback";

    @Data
    public static class AlipayConfig {
        /**
         * 应用ID
         */
        private String appId = "20210001226823456789";

        /**
         * 商户私钥
         */
        private String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC5J8Kx8XhVqjL";

        /**
         * 支付宝公钥
         */
        private String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA";

        /**
         * 网关地址
         */
        private String gatewayUrl = "https://openapi.alipay.com/gateway.do";

        /**
         * 签名算法
         */
        private String signType = "RSA2";

        /**
         * 字符编码
         */
        private String charset = "UTF-8";

        /**
         * 格式
         */
        private String format = "json";

        /**
         * 应用私钥证书路径
         */
        private String appPrivateKeyPath = "";

        /**
         * 支付宝公钥证书路径
         */
        private String alipayPublicKeyPath = "";

        /**
         * 是否沙箱环境
         */
        private boolean sandbox = true;

        /**
         * 应用公钥证书路径
         */
        private String appCertPublicKeyPath = "";

        /**
         * 支付宝根证书路径
         */
        private String alipayRootCertPath = "";

        /**
         * 支付宝公钥证书SN
         */
        private String alipayPublicCertSN = "";

        /**
         * 支付宝根证书SN
         */
        private String alipayRootCertSN = "";
    }

    @Data
    public static class WechatConfig {
        /**
         * 是否启用真实微信支付接入
         */
        private boolean enabled = false;

        /**
         * JSAPI/小程序使用的应用ID
         */
        private String appId = "";

        /**
         * 商户号
         */
        private String mchid = "190000****";

        /**
         * 子商户号（服务商模式可选）
         */
        private String subMchid = "";

        /**
         * 商户API私钥路径
         */
        private String privateKeyPath = "";

        /**
         * 商户证书序列号
         */
        private String merchantSerialNumber = "";

        /**
         * 商户API私钥
         */
        private String privateKey = "";

        /**
         * APIv3密钥
         */
        private String apiV3Key = "";

        /**
         * APIv2密钥（旧版本）
         */
        private String apiV2Key = "";

        /**
         * 商户API证书
         */
        private String merchantCertificate = "";

        /**
         * 平台证书
         */
        private String wechatPayCertificate = "";

        /**
         * API证书序列号
         */
        private String apiClientSerial = "";

        /**
         * 平台证书序列号
         */
        private String apiCertificateSerial = "";

        /**
         * 微信支付接口地址
         */
        private String baseUrl = "https://api.mch.weixin.qq.com";

        /**
         * 支付通知地址
         */
        private String notifyUrl = "";

        /**
         * 退款结果通知地址
         */
        private String refundNotifyUrl = "";

        /**
         * 加载私钥内容
         */
        public String resolvePrivateKey() {
            if (StringUtils.hasText(privateKey)) {
                return privateKey;
            }
            if (!StringUtils.hasText(privateKeyPath)) {
                return "";
            }
            try {
                Path path = Paths.get(privateKeyPath);
                if (!path.isAbsolute()) {
                    path = Paths.get(System.getProperty("user.dir")).resolve(privateKeyPath).normalize();
                }
                if (Files.exists(path)) {
                    return Files.readString(path);
                }
                log.warn("指定的微信私钥路径不存在: {}", path);
            } catch (Exception ex) {
                log.error("读取微信私钥失败: {}", privateKeyPath, ex);
            }
            return "";
        }

        /**
         * 判定配置是否满足真实接入条件
         */
        public boolean isFullyConfigured() {
            return enabled
                && StringUtils.hasText(appId)
                && StringUtils.hasText(mchid)
                && StringUtils.hasText(merchantSerialNumber)
                && StringUtils.hasText(apiV3Key)
                && StringUtils.hasText(resolvePrivateKey());
        }
    }

    /**
     * 创建支付宝客户端 - 暂时简化，后续集成真实SDK时再完善
     */
    @Bean
    public AlipayClient alipayClient() {
        try {
            // 暂时使用基础的构造方法，后续根据实际SDK需求完善
            log.info("创建支付宝客户端: appId={}, sandbox={}", alipay.getAppId(), alipay.isSandbox());
            return new DefaultAlipayClient(
                alipay.getGatewayUrl(),
                alipay.getAppId(),
                alipay.getPrivateKey(),
                alipay.getFormat(),
                alipay.getCharset(),
                alipay.getAlipayPublicKey(),
                alipay.getSignType()
            );
        } catch (Exception e) {
            log.error("创建支付宝客户端失败", e);
            throw new RuntimeException("创建支付宝客户端失败", e);
        }
    }

    /**
     * 创建微信支付客户端 - 暂时移除，后续集成真实SDK时再添加
     */
    // @Bean
    // public com.github.wechatpay.java.core.WechatPayClient wechatPayClient() {
    //     // 微信支付客户端将在后续版本中集成真实SDK时实现
    //     return null;
    // }
}
