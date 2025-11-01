package com.evcs.payment.service;

import com.evcs.payment.dto.RefundCallbackRequest;

/**
 * 退款回调服务接口
 */
public interface IRefundCallbackService {

    /**
     * 处理退款回调
     *
     * @param callbackRequest 退款回调请求
     * @return 是否处理成功
     */
    boolean handleRefundCallback(RefundCallbackRequest callbackRequest);

    /**
     * 验证退款回调签名
     *
     * @param callbackRequest 退款回调请求
     * @return 签名是否有效
     */
    boolean verifyRefundCallbackSignature(RefundCallbackRequest callbackRequest);

    /**
     * 解析支付宝退款回调
     *
     * @param params 回调参数
     * @return 退款回调请求
     */
    RefundCallbackRequest parseAlipayRefundCallback(java.util.Map<String, String> params);

    /**
     * 解析微信退款回调
     *
     * @param xmlData 回调XML数据
     * @return 退款回调请求
     */
    RefundCallbackRequest parseWechatRefundCallback(String xmlData);
}