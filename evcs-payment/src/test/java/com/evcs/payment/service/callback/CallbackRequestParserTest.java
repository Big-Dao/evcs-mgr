package com.evcs.payment.service.callback;

import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.CallbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回调报文解析服务测试：微信 API v3 解密、字段映射与验签原始数据顺序。
 */
class CallbackRequestParserTest {

    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";

    private PaymentConfig paymentConfig;
    private CallbackRequestParser parser;

    @BeforeEach
    void setUp() {
        paymentConfig = new PaymentConfig();
        paymentConfig.getWechat().setApiV3Key(API_V3_KEY);
        parser = new CallbackRequestParser(paymentConfig, new ObjectMapper());
    }

    private String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(API_V3_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new GCMParameterSpec(128, "nonce1".getBytes(StandardCharsets.UTF_8)));
        cipher.updateAAD("transaction".getBytes(StandardCharsets.UTF_8));
        byte[] bytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String wechatBody(String ciphertext) {
        return "{\"event_type\":\"TRANSACTION.SUCCESS\","
                + "\"resource\":{\"ciphertext\":\"" + ciphertext + "\","
                + "\"nonce\":\"nonce1\",\"associated_data\":\"transaction\"}}";
    }

    @Test
    @DisplayName("微信回调 - AES/GCM 解密后应正确映射统一字段")
    void parseWechatShouldDecryptAndMapFields() throws Exception {
        String plain = "{\"out_trade_no\":\"OT123\",\"transaction_id\":\"TX456\","
                + "\"trade_state\":\"SUCCESS\",\"amount\":{\"payer_total\":100},"
                + "\"payer\":{\"openid\":\"o-1\"},\"success_time\":\"2026-09-03T10:00:00\"}";
        String body = wechatBody(encrypt(plain));

        Map<String, String> headers = Map.of("Wechatpay-Signature", "sig1", "Wechatpay-Signature-Type", "RSA");
        CallbackRequest request = parser.parseWechat(body, headers);

        assertNotNull(request);
        assertEquals("wechat", request.getChannel());
        assertEquals("TRANSACTION.SUCCESS", request.getEventType());
        assertEquals("OT123", request.getTradeNo());
        assertEquals("TX456", request.getOutTradeNo());
        assertEquals("SUCCESS", request.getTradeStatus());
        assertEquals("100", request.getTotalAmount());
        assertEquals("o-1", request.getBuyerId());
        assertEquals("sig1", request.getSign());
        assertEquals("RSA", request.getSignType());
    }

    @Test
    @DisplayName("微信回调 - 密文被篡改时应返回 null（fail-closed）")
    void parseWechatShouldReturnNullWhenCiphertextTampered() {
        String body = wechatBody(Base64.getEncoder().encodeToString("tampered".getBytes(StandardCharsets.UTF_8)));

        assertNull(parser.parseWechat(body, Map.of()));
    }

    @Test
    @DisplayName("微信回调 - 未配置 API v3 密钥时应返回 null")
    void parseWechatShouldReturnNullWhenKeyMissing() {
        paymentConfig.getWechat().setApiV3Key("");
        String body = wechatBody("AAAA");

        assertNull(parser.parseWechat(body, Map.of()));
    }

    @Test
    @DisplayName("支付宝回调 - rawData 应保持参数枚举顺序（验签依赖）")
    void parseAlipayShouldKeepParameterOrderInRawData() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("gmt_payment", "2026-09-03");
        params.put("out_trade_no", "OT789");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign", "abc");

        CallbackRequest request = parser.parseAlipay(params, Map.of());

        assertEquals("OT789", request.getOutTradeNo());
        assertEquals("TRADE_SUCCESS", request.getTradeStatus());
        assertEquals("gmt_payment=2026-09-03&out_trade_no=OT789&trade_status=TRADE_SUCCESS&sign=abc",
                request.getRawData(), "验签原始数据必须保持调用方给定顺序");
        assertTrue(request.getExtraParams().containsKey("sign"));
    }

    @Test
    @DisplayName("微信应答报文 - 成功与失败格式正确")
    void wechatResponseXmlShouldBeWellFormed() {
        assertEquals("<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>",
                parser.wechatSuccessResponse());
        assertTrue(parser.wechatFailureResponse("解析失败").contains("解析失败"));
        assertTrue(parser.wechatFailureResponse(null).contains("FAIL"));
    }
}
