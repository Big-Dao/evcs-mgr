package com.evcs.payment.service.callback;

import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.dto.CallbackResponse;

/**
 * 支付回调服务接口
 *
 * 统一处理各支付渠道的回调通知
 */
public interface PaymentCallbackService {

    /**
     * 处理支付回调
     *
     * @param channel 支付渠道 (alipay, wechat)
     * @param request 回调请求
     * @return 回调响应
     */
    CallbackResponse handleCallback(String channel, CallbackRequest request);

    /**
     * 验证回调签名
     *
     * @param channel 支付渠道
     * @param request 回调请求
     * @return 是否验证通过
     */
    boolean verifySignature(String channel, CallbackRequest request);
}