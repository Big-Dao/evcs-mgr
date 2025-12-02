package com.evcs.payment.service.channel;

import com.evcs.payment.config.PaymentConfig;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;
import java.util.Base64;

/**
 * 微信支付SDK客户端工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPayClientFactory {

    private final PaymentConfig paymentConfig;

    private final Object initLock = new Object();

    private volatile boolean initialized;
    private volatile RSAAutoCertificateConfig certificateConfig;
    private volatile JsapiServiceExtension jsapiService;
    private volatile NativePayService nativePayService;
    private volatile RefundService refundService;
    private volatile NotificationParser notificationParser;

    /**
     * 查询是否具备真实接入条件
     */
    public boolean isActive() {
        return paymentConfig != null
            && paymentConfig.getWechat() != null
            && paymentConfig.getWechat().isFullyConfigured();
    }

    /**
     * 获取JSAPI服务
     */
    public Optional<JsapiServiceExtension> getJsapiService() {
        if (!isActive()) {
            return Optional.empty();
        }
        ensureInitialized();
        return Optional.ofNullable(jsapiService);
    }

    /**
     * 获取Native支付服务
     */
    public Optional<NativePayService> getNativePayService() {
        if (!isActive()) {
            return Optional.empty();
        }
        ensureInitialized();
        return Optional.ofNullable(nativePayService);
    }

    /**
     * 获取退款服务
     */
    public Optional<RefundService> getRefundService() {
        if (!isActive()) {
            return Optional.empty();
        }
        ensureInitialized();
        return Optional.ofNullable(refundService);
    }

    /**
     * 获取通知解析器
     */
    public Optional<NotificationParser> getNotificationParser() {
        if (!isActive()) {
            return Optional.empty();
        }
        ensureInitialized();
        return Optional.ofNullable(notificationParser);
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (initLock) {
            if (initialized) {
                return;
            }
            initialize();
            initialized = true;
        }
    }

    private void initialize() {
        PaymentConfig.WechatConfig config = paymentConfig.getWechat();
        if (config == null || !config.isFullyConfigured()) {
            log.warn("微信支付配置不完整，无法初始化SDK");
            return;
        }
        try {
            PrivateKey privateKey = loadPrivateKey(config.resolvePrivateKey());
            certificateConfig = new RSAAutoCertificateConfig.Builder()
                .merchantId(config.getMchid())
                .privateKey(privateKey)
                .merchantSerialNumber(config.getMerchantSerialNumber())
                .apiV3Key(config.getApiV3Key())
                .build();

            jsapiService = new JsapiServiceExtension.Builder()
                .config(certificateConfig)
                .build();
            nativePayService = new NativePayService.Builder()
                .config(certificateConfig)
                .build();
            refundService = new RefundService.Builder()
                .config(certificateConfig)
                .build();
            notificationParser = new NotificationParser(certificateConfig);

            log.info("微信支付SDK初始化完成: mchid={}, serial={}",
                config.getMchid(), config.getMerchantSerialNumber());
        } catch (Exception ex) {
            log.error("初始化微信支付SDK失败", ex);
            releaseResources();
        }
    }

    private PrivateKey loadPrivateKey(String privateKeyPem) {
        if (!StringUtils.hasText(privateKeyPem)) {
            throw new IllegalArgumentException("微信商户私钥未配置");
        }
        try {
            String normalized = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("解析微信商户私钥失败", ex);
        }
    }

    private void releaseResources() {
        certificateConfig = null;
        jsapiService = null;
        nativePayService = null;
        refundService = null;
        notificationParser = null;
        initialized = false;
    }
}
