package com.evcs.common.exception;

import com.evcs.common.result.ResultCode;

/**
 * 第三方服务异常
 * <p>用于表示第三方服务调用失败
 */
public class ThirdPartyServiceException extends BaseException {

    private final String thirdParty;

    public ThirdPartyServiceException(String thirdParty, String message) {
        super(ResultCode.THIRD_PARTY_ERROR, String.format("%s服务调用失败: %s", thirdParty, message));
        this.thirdParty = thirdParty;
    }

    public ThirdPartyServiceException(String thirdParty, String message, Throwable cause) {
        super(ResultCode.THIRD_PARTY_ERROR, String.format("%s服务调用失败: %s", thirdParty, message), cause);
        this.thirdParty = thirdParty;
    }

    public ThirdPartyServiceException(ResultCode resultCode, String thirdParty, String message) {
        super(resultCode, String.format("%s服务调用失败: %s", thirdParty, message));
        this.thirdParty = thirdParty;
    }

    public ThirdPartyServiceException(ResultCode resultCode, String thirdParty, String message, Throwable cause) {
        super(resultCode, String.format("%s服务调用失败: %s", thirdParty, message), cause);
        this.thirdParty = thirdParty;
    }

    public String getThirdParty() {
        return thirdParty;
    }
}
