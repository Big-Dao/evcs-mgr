package com.evcs.payment.controller;

import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.dto.CallbackResponse;
import com.evcs.payment.dto.RefundCallbackRequest;
import com.evcs.payment.service.callback.CallbackRequestParser;
import com.evcs.payment.service.callback.PaymentCallbackService;
import com.evcs.payment.service.IRefundCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器
 *
 * 处理来自支付宝和微信支付的回调通知。
 * 只负责 HTTP 报文读取与应答；渠道解析/解密与业务处理在服务层完成。
 */
@Tag(name = "支付回调", description = "处理各支付平台的回调通知")
@Slf4j
@RestController
@RequestMapping("/api/payment/callback")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentCallbackService paymentCallbackService;
    private final IRefundCallbackService refundCallbackService;
    private final CallbackRequestParser callbackRequestParser;

    /**
     * 支付宝支付回调
     */
    @PostMapping("/alipay")
    @Operation(summary = "支付宝支付回调")
    public ResponseEntity<String> alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝支付回调");

        try {
            CallbackRequest callbackRequest = callbackRequestParser.parseAlipay(
                    extractRequestParams(request), extractRequestHeaders(request));
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
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            CallbackRequest callbackRequest = callbackRequestParser.parseWechat(
                    body, extractRequestHeaders(request));
            if (callbackRequest == null) {
                log.warn("微信回调解析失败，返回FAIL");
                return ResponseEntity.ok(callbackRequestParser.wechatFailureResponse("解析失败"));
            }
            CallbackResponse response = paymentCallbackService.handleCallback("wechat", callbackRequest);

            log.info("微信回调处理完成: success={}, message={}",
                    response.isSuccess(), response.getMessage());

            return ResponseEntity.ok(response.getResponseContent());

        } catch (Exception e) {
            log.error("处理微信回调异常", e);
            return ResponseEntity.ok(callbackRequestParser.wechatFailureResponse("处理失败"));
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
                return ResponseEntity.ok(callbackRequestParser.wechatFailureResponse("请求体为空"));
            }

            RefundCallbackRequest callbackRequest = refundCallbackService.parseWechatRefundCallback(requestBody);
            if (callbackRequest == null) {
                log.warn("微信退款回调解析失败");
                return ResponseEntity.ok(callbackRequestParser.wechatFailureResponse("解析失败"));
            }

            boolean success = refundCallbackService.handleRefundCallback(callbackRequest);
            if (success) {
                log.info("微信退款回调处理成功: outTradeNo={}, outRequestNo={}",
                    callbackRequest.getOutTradeNo(), callbackRequest.getOutRequestNo());
                return ResponseEntity.ok(callbackRequestParser.wechatSuccessResponse());
            }

            log.warn("微信退款回调处理失败: outTradeNo={}, outRequestNo={}",
                callbackRequest.getOutTradeNo(), callbackRequest.getOutRequestNo());
            return ResponseEntity.ok(callbackRequestParser.wechatFailureResponse("处理失败"));

        } catch (Exception e) {
            log.error("处理微信退款回调异常", e);
            return ResponseEntity.ok(callbackRequestParser.wechatFailureResponse("内部错误"));
        }
    }

    /**
     * 提取请求参数（保持枚举顺序：支付宝验签 rawData 依赖参数顺序）
     */
    private Map<String, String> extractRequestParams(HttpServletRequest request) {
        Map<String, String> params = new java.util.LinkedHashMap<>();

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
}
