package com.evcs.payment.service.callback;

import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.CallbackRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 支付回调报文解析服务：渠道报文 → 统一 {@link CallbackRequest}。
 *
 * <p>渠道适配（微信 API v3 AES/GCM 解密、字段映射、应答报文组装）属于
 * 安全与渠道协议逻辑，归属服务层；HTTP 参数/请求头的读取由 Controller 完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackRequestParser {

    private final PaymentConfig paymentConfig;
    private final ObjectMapper objectMapper;

    /**
     * 解析支付宝回调参数（参数表已由 Controller 从请求中提取）。
     */
    public CallbackRequest parseAlipay(Map<String, String> params, Map<String, String> headers) {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setChannel("alipay");

        Map<String, String> safeParams = params != null ? params : new HashMap<>();

        // 构建原始数据用于签名验证
        StringBuilder rawDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : safeParams.entrySet()) {
            if (rawDataBuilder.length() > 0) {
                rawDataBuilder.append("&");
            }
            rawDataBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }

        callbackRequest.setTradeNo(safeParams.get("trade_no"));
        callbackRequest.setOutTradeNo(safeParams.get("out_trade_no"));
        callbackRequest.setTradeStatus(safeParams.get("trade_status"));
        callbackRequest.setTotalAmount(safeParams.get("total_amount"));
        callbackRequest.setBuyerId(safeParams.get("buyer_id"));
        callbackRequest.setGmtPayment(safeParams.get("gmt_payment"));
        callbackRequest.setSign(safeParams.get("sign"));
        callbackRequest.setSignType(safeParams.get("sign_type"));
        callbackRequest.setRawData(rawDataBuilder.toString());
        callbackRequest.setExtraParams(safeParams);
        callbackRequest.setHeaders(headers);

        log.debug("构建回调请求: channel=alipay, tradeNo={}, tradeStatus={}",
                callbackRequest.getTradeNo(), callbackRequest.getTradeStatus());
        return callbackRequest;
    }

    /**
     * 解析微信回调报文（请求体已由 Controller 读取）；解析失败返回 null。
     */
    public CallbackRequest parseWechat(String body, Map<String, String> headers) {
        try {
            if (!StringUtils.hasText(body)) {
                log.warn("微信回调体为空");
                return null;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode resourceNode = root.path("resource");
            JsonNode decryptedNode = decryptWechatResource(resourceNode);
            if (decryptedNode == null) {
                return null;
            }

            CallbackRequest callbackRequest = new CallbackRequest();
            callbackRequest.setChannel("wechat");
            callbackRequest.setRawData(body);
            callbackRequest.setHeaders(headers);
            callbackRequest.setEventType(root.path("event_type").asText(null));
            callbackRequest.setTradeNo(decryptedNode.path("out_trade_no").asText(null));
            callbackRequest.setOutTradeNo(decryptedNode.path("transaction_id").asText(null));
            callbackRequest.setTradeStatus(decryptedNode.path("trade_state").asText(null));
            callbackRequest.setTotalAmount(extractJsonText(decryptedNode, "amount", "payer_total"));
            callbackRequest.setBuyerId(extractJsonText(decryptedNode, "payer", "openid"));
            callbackRequest.setGmtPayment(decryptedNode.path("success_time").asText(null));
            callbackRequest.setSign(headers != null ? headers.get("Wechatpay-Signature") : null);
            callbackRequest.setSignType(headers != null ? headers.get("Wechatpay-Signature-Type") : null);

            Map<String, Object> extra = objectMapper.convertValue(decryptedNode,
                new TypeReference<Map<String, Object>>() {});
            Map<String, String> flattened = new HashMap<>();
            extra.forEach((key, value) -> flattened.put(key, value != null ? value.toString() : null));
            callbackRequest.setExtraParams(flattened);

            log.debug("解析微信回调成功: tradeNo={}, status={}",
                callbackRequest.getTradeNo(), callbackRequest.getTradeStatus());
            return callbackRequest;
        } catch (Exception ex) {
            log.error("解析微信支付回调失败", ex);
            return null;
        }
    }

    public String wechatSuccessResponse() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    public String wechatFailureResponse(String message) {
        return String.format("<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[%s]]></return_msg></xml>",
            StringUtils.hasText(message) ? message : "FAIL");
    }

    private JsonNode decryptWechatResource(JsonNode resourceNode) {
        if (resourceNode == null || resourceNode.isMissingNode()) {
            log.warn("微信回调缺少resource节点");
            return null;
        }
        PaymentConfig.WechatConfig wechatConfig = paymentConfig.getWechat();
        if (wechatConfig == null || !StringUtils.hasText(wechatConfig.getApiV3Key())) {
            log.warn("未配置微信API v3密钥，无法解密回调数据");
            return null;
        }
        String apiV3Key = wechatConfig.getApiV3Key();
        try {
            String cipherText = resourceNode.path("ciphertext").asText();
            String nonce = resourceNode.path("nonce").asText();
            String associatedData = resourceNode.path("associated_data").asText("");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] decoded = Base64.getDecoder().decode(cipherText);
            byte[] plainBytes = cipher.doFinal(decoded);
            String plainText = new String(plainBytes, StandardCharsets.UTF_8);
            return objectMapper.readTree(plainText);
        } catch (Exception ex) {
            log.error("解密微信回调resource失败", ex);
            return null;
        }
    }

    private String extractJsonText(JsonNode node, String parentField, String childField) {
        if (node == null) {
            return null;
        }
        JsonNode parent = node.path(parentField);
        if (parent.isMissingNode()) {
            return null;
        }
        JsonNode child = parent.path(childField);
        return child.isMissingNode() ? null : child.asText(null);
    }
}
