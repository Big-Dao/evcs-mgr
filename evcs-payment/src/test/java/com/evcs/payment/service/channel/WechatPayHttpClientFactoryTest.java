package com.evcs.payment.service.channel;

import com.evcs.payment.config.PaymentConfig;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.auth.Credential;
import com.wechat.pay.java.core.auth.Validator;
import com.wechat.pay.java.core.http.HttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class WechatPayHttpClientFactoryTest {

    @Test
    @DisplayName("微信支付SDK HttpClient - 应显式配置连接/读取/写入超时")
    void testBuildHttpClient_shouldConfigureTimeouts() throws Exception {
        // Arrange
        PaymentConfig.WechatConfig wechatConfig = new PaymentConfig.WechatConfig();
        wechatConfig.setConnectTimeoutMs(1234);
        wechatConfig.setReadTimeoutMs(5678);
        wechatConfig.setWriteTimeoutMs(9012);

        Config dummyConfig = new DummyConfig();

        // Act
        HttpClient httpClient = WechatPayHttpClientFactory.buildHttpClient(dummyConfig, wechatConfig);

        // Assert
        assertNotNull(httpClient, "HttpClient must not be null");

        Object okHttpClient = unwrapOkHttpClient(httpClient);
        assertNotNull(okHttpClient, "Underlying OkHttpClient must be present");

        assertEquals(
            1234,
            (int) okHttpClient.getClass().getMethod("connectTimeoutMillis").invoke(okHttpClient),
            "connectTimeoutMillis should match config"
        );
        assertEquals(
            5678,
            (int) okHttpClient.getClass().getMethod("readTimeoutMillis").invoke(okHttpClient),
            "readTimeoutMillis should match config"
        );
        assertEquals(
            9012,
            (int) okHttpClient.getClass().getMethod("writeTimeoutMillis").invoke(okHttpClient),
            "writeTimeoutMillis should match config"
        );
    }

    private static Object unwrapOkHttpClient(HttpClient httpClient) throws Exception {
        Objects.requireNonNull(httpClient, "httpClient must not be null");

        Field okHttpClientField;
        try {
            okHttpClientField = httpClient.getClass().getDeclaredField("okHttpClient");
        } catch (NoSuchFieldException e) {
            return null;
        }
        okHttpClientField.setAccessible(true);
        return okHttpClientField.get(httpClient);
    }

    private static final class DummyConfig implements Config {
        @Override
        public com.wechat.pay.java.core.cipher.PrivacyEncryptor createEncryptor() {
            return null;
        }

        @Override
        public com.wechat.pay.java.core.cipher.PrivacyDecryptor createDecryptor() {
            return null;
        }

        @Override
        public Credential createCredential() {
            return new Credential() {
                @Override
                public String getSchema() {
                    return "WECHATPAY2-SHA256-RSA2048";
                }

                @Override
                public String getMerchantId() {
                    return "1900000000";
                }

                @Override
                public String getAuthorization(java.net.URI uri, String httpMethod, String signBody) {
                    return "AUTH";
                }
            };
        }

        @Override
        public Validator createValidator() {
            return new Validator() {
                @Override
                public <T> boolean validate(com.wechat.pay.java.core.http.HttpHeaders responseHeaders, String body) {
                    return true;
                }
            };
        }

        @Override
        public com.wechat.pay.java.core.cipher.Signer createSigner() {
            return new com.wechat.pay.java.core.cipher.Signer() {
                @Override
                public com.wechat.pay.java.core.cipher.SignatureResult sign(String message) {
                    throw new UnsupportedOperationException("not needed in this test");
                }

                @Override
                public String getAlgorithm() {
                    return "SHA256withRSA";
                }
            };
        }
    }
}
