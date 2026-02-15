package com.evcs.payment.security;

import com.evcs.common.util.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("支付模块敏感数据脱敏合规测试")
class SensitiveDataMaskerComplianceTest {

    @Test
    @DisplayName("银行卡号 - 应按前4后4规则脱敏")
    void testMaskBankCard_shouldKeepFirstAndLastFourDigits() {
        String masked = SensitiveDataMasker.maskBankCard("6222021234567890");
        assertEquals("6222****7890", masked);
    }

    @Test
    @DisplayName("手机号 - 应隐藏中间4位")
    void testMaskPhone_shouldHideMiddleDigits() {
        String masked = SensitiveDataMasker.maskPhone("13812345678");
        assertEquals("138****5678", masked);
    }

    @Test
    @DisplayName("身份证号 - 应保留前6后4")
    void testMaskIdCard_shouldKeepFirstSixAndLastFour() {
        String masked = SensitiveDataMasker.maskIdCard("110101199001011234");
        assertEquals("110101********1234", masked);
    }
}
