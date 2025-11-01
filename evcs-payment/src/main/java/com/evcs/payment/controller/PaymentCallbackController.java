package com.evcs.payment.controller;

import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.dto.CallbackResponse;
import com.evcs.payment.dto.RefundCallbackRequest;
import com.evcs.payment.service.callback.PaymentCallbackService;
import com.evcs.payment.service.IRefundCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 支付宝支付回调
     */
    @PostMapping("/alipay")
    @Operation(summary = "支付宝支付回调")
    public ResponseEntity<String> alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝支付回调");

        try {
            CallbackRequest callbackRequest = buildCallbackRequest("alipay", request);
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
            CallbackRequest callbackRequest = buildCallbackRequest("wechat", request);
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
        // TODO: 实现退款回调处理
        String response = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        return ResponseEntity.ok(response);
    }

    /**
     * 构建回调请求对象
     */
    private CallbackRequest buildCallbackRequest(String channel, HttpServletRequest request) {
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

        log.debug("构建回调请求: channel={}, tradeNo={}, tradeStatus={}",
                channel, callbackRequest.getTradeNo(), callbackRequest.getTradeStatus());

        return callbackRequest;
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
}