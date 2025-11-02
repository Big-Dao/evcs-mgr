package com.evcs.payment.dto;

import lombok.Data;

/**
 * 支付回调响应
 */
@Data
public class CallbackResponse {
    /**
     * 是否处理成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应给支付平台的内容
     */
    private String responseContent;

    /**
     * HTTP响应状态码
     */
    private int statusCode;

    public static CallbackResponse success(String responseContent) {
        CallbackResponse response = new CallbackResponse();
        response.setSuccess(true);
        response.setMessage("处理成功");
        response.setResponseContent(responseContent);
        response.setStatusCode(200);
        return response;
    }

    public static CallbackResponse failure(String message) {
        CallbackResponse response = new CallbackResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatusCode(500);
        return response;
    }

    public static CallbackResponse failure(String message, int statusCode) {
        CallbackResponse response = new CallbackResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setStatusCode(statusCode);
        return response;
    }
}