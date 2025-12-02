package com.evcs.payment.controller;

import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.dto.CallbackResponse;
import com.evcs.payment.dto.RefundCallbackRequest;
import com.evcs.payment.service.callback.PaymentCallbackService;
import com.evcs.payment.service.IRefundCallbackService;
import com.evcs.payment.config.PaymentConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 支付回调控制器
 *
 * 处理来自支付宝和微信支付的回调通知
 */
@Tag(name = "支付回调", description = "处理各支付平台的回调通知")
@Slf4j
@RestController
@RequestMapping("/api/payment/callback")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentCallbackService paymentCallbackService;
    private final IRefundCallbackService refundCallbackService;
    private final PaymentConfig paymentConfig;
    private final ObjectMapper objectMapper;

    /**
     * 支付宝支付回调
     */
    @PostMapping("/alipay")
    @Operation(summary = "支付宝支付回调")
    public ResponseEntity<String> alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝支付回调");

        try {
            CallbackRequest callbackRequest = buildAlipayCallbackRequest(request);
            CallbackResponse response = paymentCallbackService.handleCallback("alipay", callbackRequest);

            log.info("支付宝回调处理完成: success={}, message={}",
                    response.isSuccess(), response.getMessage());

            return ResponseEntity.ok(response.getResponseContent());

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return ResponseEntity.ok("failure");
        }
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/wechat")
    @Operation(summary = "微信支付回调")
    public ResponseEntity<String> wechatCallback(HttpServletRequest request) {
        log.info("收到微信支付回调");

        try {
            CallbackRequest callbackRequest = buildWechatCallbackRequest(request);
            if (callbackRequest == null) {
                log.warn("微信回调解析失败，返回FAIL");
                return ResponseEntity.ok(buildWechatFailureResponse("解析失败"));
            }
            CallbackResponse response = paymentCallbackService.handleCallback("wechat", callbackRequest);

            log.info("微信回调处理完成: success={}, message={}",
                    response.isSuccess(), response.getMessage());

            return ResponseEntity.ok(response.getResponseContent());

        } catch (Exception e) {
            log.error("处理微信回调异常", e);
            String failureResponse = "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
            return ResponseEntity.ok(failureResponse);
        }
    }

    /**
     * 支付宝退款回调
     */
    @PostMapping("/alipay/refund")
    @Operation(summary = "支付宝退款回调")
    public ResponseEntity<String> alipayRefundCallback(HttpServletRequest request) {
        log.info("收到支付宝退款回调");

        try {
            // 解析回调参数
            Map<String, String> params = extractRequestParams(request);

            // 解析退款回调请求
            RefundCallbackRequest refundCallbackRequest = refundCallbackService.parseAlipayRefundCallback(params);

            // 处理退款回调
            boolean success = refundCallbackService.handleRefundCallback(refundCallbackRequest);

            if (success) {
                log.info("支付宝退款回调处理成功");
                return ResponseEntity.ok("success");
            } else {
                log.error("支付宝退款回调处理失败");
                return ResponseEntity.ok("fail");
            }

        } catch (Exception e) {
            log.error("处理支付宝退款回调异常", e);
            return ResponseEntity.ok("fail");
        }
    }

    /**
     * 微信退款回调
     */
    @PostMapping("/wechat/refund")
    @Operation(summary = "微信退款回调")
    public ResponseEntity<String> wechatRefundCallback(HttpServletRequest request) {
        log.info("收到微信退款回调");
        try {
            String requestBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (!StringUtils.hasText(requestBody)) {
                log.warn("微信退款回调请求体为空");
                return ResponseEntity.ok(buildWechatFailureResponse("请求体为空"));
            }

            RefundCallbackRequest callbackRequest = refundCallbackService.parseWechatRefundCallback(requestBody);
            if (callbackRequest == null) {
                log.warn("微信退款回调解析失败");
                return ResponseEntity.ok(buildWechatFailureResponse("解析失败"));
            }

            boolean success = refundCallbackService.handleRefundCallback(callbackRequest);
            if (success) {
                log.info("微信退款回调处理成功: outTradeNo={}, outRequestNo={}",
                    callbackRequest.getOutTradeNo(), callbackRequest.getOutRequestNo());
                return ResponseEntity.ok(buildWechatSuccessResponse());
            }

            log.warn("微信退款回调处理失败: outTradeNo={}, outRequestNo={}",
                callbackRequest.getOutTradeNo(), callbackRequest.getOutRequestNo());
            return ResponseEntity.ok(buildWechatFailureResponse("处理失败"));

        } catch (Exception e) {
            log.error("处理微信退款回调异常", e);
            return ResponseEntity.ok(buildWechatFailureResponse("内部错误"));
        }
    }

    /**
     * 构建回调请求对象
     */
    private CallbackRequest buildAlipayCallbackRequest(HttpServletRequest request) {
        String channel = "alipay";
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setChannel(channel);

        Map<String, String> params = new HashMap<>();

        // 获取所有请求参数
        Enumeration<String> parameterNames = request.getParameterNames();
        StringBuilder rawDataBuilder = new StringBuilder();

        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);

            // 构建原始数据用于签名验证
            if (rawDataBuilder.length() > 0) {
                rawDataBuilder.append("&");
            }
            rawDataBuilder.append(paramName).append("=").append(paramValue);
        }

        // 设置常用字段
        callbackRequest.setTradeNo(params.get("trade_no"));
        callbackRequest.setOutTradeNo(params.get("out_trade_no"));
        callbackRequest.setTradeStatus(params.get("trade_status"));
        callbackRequest.setTotalAmount(params.get("total_amount"));
        callbackRequest.setBuyerId(params.get("buyer_id"));
        callbackRequest.setGmtPayment(params.get("gmt_payment"));
        callbackRequest.setSign(params.get("sign"));
        callbackRequest.setSignType(params.get("sign_type"));
        callbackRequest.setRawData(rawDataBuilder.toString());
        callbackRequest.setExtraParams(params);

        callbackRequest.setHeaders(extractRequestHeaders(request));

        log.debug("构建回调请求: channel={}, tradeNo={}, tradeStatus={}",
                channel, callbackRequest.getTradeNo(), callbackRequest.getTradeStatus());

        return callbackRequest;
    }

    private CallbackRequest buildWechatCallbackRequest(HttpServletRequest request) {
        try {
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (!StringUtils.hasText(body)) {
                log.warn("微信回调体为空");
                return null;
            }

            Map<String, String> headers = extractRequestHeaders(request);
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
            callbackRequest.setSign(headers.get("Wechatpay-Signature"));
            callbackRequest.setSignType(headers.get("Wechatpay-Signature-Type"));

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

    /**
     * 提取请求参数
     */
    private Map<String, String> extractRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();

        // 获取所有请求参数
        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String paramValue = request.getParameter(paramName);
            params.put(paramName, paramValue);
        }

        return params;
    }

    private Map<String, String> extractRequestHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private String buildWechatSuccessResponse() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    private String buildWechatFailureResponse(String message) {
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
