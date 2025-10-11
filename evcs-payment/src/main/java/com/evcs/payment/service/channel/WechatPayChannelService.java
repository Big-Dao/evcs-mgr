package com.evcs.payment.service.channel;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 微信支付渠道服务（模拟实现）
 * 
 * TODO: 集成微信支付SDK
 * 1. 添加 wechatpay-apache-httpclient 依赖
 * 2. 配置沙箱环境参数（商户号、API密钥、证书）
 * 3. 实现实际的微信支付API调用
 */
@Slf4j
@Service
public class WechatPayChannelService implements IPaymentChannel {

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("创建微信支付订单: orderId={}, amount={}, method={}", 
            request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        // TODO: 调用微信支付SDK创建支付
        // 这里使用模拟实现
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(UUID.randomUUID().toString());
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);

        // 模拟生成支付参数
        switch (request.getPaymentMethod()) {
            case WECHAT_JSAPI:
                // 小程序支付返回支付参数
                response.setPayParams("{\"appId\":\"mock\",\"timeStamp\":\"" + 
                    System.currentTimeMillis() + "\",\"nonceStr\":\"" + 
                    UUID.randomUUID().toString() + "\"}");
                break;
            case WECHAT_NATIVE:
                // Native扫码支付返回二维码URL
                response.setPayUrl("weixin://wxpay/bizpayurl?pr=" + response.getTradeNo());
                break;
            default:
                throw new IllegalArgumentException("不支持的支付方式: " + request.getPaymentMethod());
        }

        log.info("微信支付订单创建成功: tradeNo={}", response.getTradeNo());
        return response;
    }

    @Override
    public PaymentResponse queryPayment(String tradeNo) {
        log.info("查询微信支付状态: tradeNo={}", tradeNo);

        // TODO: 调用微信支付SDK查询支付状态
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(tradeNo);
        response.setStatus(PaymentStatus.SUCCESS);

        return response;
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("发起微信退款: paymentId={}, amount={}", 
            request.getPaymentId(), request.getRefundAmount());

        // TODO: 调用微信支付SDK发起退款
        RefundResponse response = new RefundResponse();
        response.setRefundNo(UUID.randomUUID().toString());
        response.setRefundAmount(request.getRefundAmount());
        response.setRefundStatus("SUCCESS");

        log.info("微信退款成功: refundNo={}", response.getRefundNo());
        return response;
    }

    @Override
    public boolean verifySignature(String data, String signature) {
        // TODO: 使用微信支付平台证书验证签名
        log.info("验证微信支付签名");
        return true; // 模拟验证通过
    }

    @Override
    public String getChannelName() {
        return "wechat";
    }
}
