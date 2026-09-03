package com.evcs.protocol.service;

import com.evcs.protocol.config.ProtocolProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 云快充签名验证器安全测试。
 *
 * <p>安全基线要求：签名密钥必填、无默认值；密钥缺失时验签必须 fail-closed，
 * 而不是用空密钥/占位密钥继续计算 HMAC（否则仓库里的占位字面量即可伪造回调签名）。
 */
class CloudChargeSignatureValidatorTest {

    private static final String SIGN_STRING =
        "requestId=req-1&apiVersion=3.0&timestamp=2026-09-03T10:00:00&deviceCode=DEVICE_1";

    private CloudChargeSignatureValidator validatorWithSecret(String secret) {
        ProtocolProperties properties = new ProtocolProperties();
        ProtocolProperties.CloudChargeConfig cloudCharge = new ProtocolProperties.CloudChargeConfig();
        cloudCharge.setAppSecret(secret);
        properties.setCloudCharge(cloudCharge);
        return new CloudChargeSignatureValidator(properties);
    }

    private CloudChargeSignatureValidator.CloudChargeRequest requestSignedWith(String secret) {
        CloudChargeSignatureValidator.CloudChargeRequest request = new CloudChargeSignatureValidator.CloudChargeRequest();
        request.setRequestId("req-1");
        request.setApiVersion("3.0");
        request.setTimestamp("2026-09-03T10:00:00");
        request.setDeviceCode("DEVICE_1");
        request.setSignature(hmacSha256(secret, SIGN_STRING));
        return request;
    }

    @Test
    @DisplayName("有效密钥 - 合法签名应通过验证")
    void shouldValidateSignatureWithConfiguredSecret() {
        CloudChargeSignatureValidator validator =
            validatorWithSecret("a-very-secret-and-random-vendor-key-0123456789");

        assertTrue(validator.validateSignature(requestSignedWith("a-very-secret-and-random-vendor-key-0123456789")));
    }

    @Test
    @DisplayName("有效密钥 - 篡改的签名应被拒绝")
    void shouldRejectTamperedSignature() {
        CloudChargeSignatureValidator validator =
            validatorWithSecret("a-very-secret-and-random-vendor-key-0123456789");

        CloudChargeSignatureValidator.CloudChargeRequest request = requestSignedWith("a-very-secret-and-random-vendor-key-0123456789");
        request.setSignature("deadbeef");

        assertFalse(validator.validateSignature(request));
    }

    @Test
    @DisplayName("密钥为空 - 验签必须 fail-closed")
    void shouldFailClosedWhenSecretIsBlank() {
        CloudChargeSignatureValidator validator = validatorWithSecret("");

        // 请求用其他密钥正常签名；验签方密钥为空时必须拒绝
        CloudChargeSignatureValidator.CloudChargeRequest request =
            requestSignedWith("some-other-valid-secret-0123456789");

        assertFalse(validator.validateSignature(request), "空密钥时 validateSignature 应返回 false");
    }

    @Test
    @DisplayName("密钥为空 - 生成外发签名应显式抛出配置异常（而非底层 JCE 异常）")
    void shouldThrowWhenGeneratingSignatureWithBlankSecret() {
        CloudChargeSignatureValidator validator = validatorWithSecret("");

        CloudChargeSignatureValidator.CloudChargeRequest request =
            requestSignedWith("some-other-valid-secret-0123456789");

        assertThrows(IllegalStateException.class,
            () -> validator.generateSignature(request),
            "空密钥时 generateSignature 应快速失败并说明密钥未配置");
    }

    @Test
    @DisplayName("密钥未配置(null) - 验签必须 fail-closed")
    void shouldFailClosedWhenSecretIsNull() {
        CloudChargeSignatureValidator validator = validatorWithSecret(null);

        assertFalse(validator.validateSignature(requestSignedWith("any-secret")));
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
