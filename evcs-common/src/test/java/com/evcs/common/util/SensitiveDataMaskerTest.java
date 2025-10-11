package com.evcs.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskerTest {

    @Test
    void maskPhone_WithValidPhone_ReturnsPartiallyMasked() {
        String phone = "13812341234";
        String masked = SensitiveDataMasker.maskPhone(phone);
        assertEquals("138****1234", masked);
    }

    @Test
    void maskPhone_WithInvalidPhone_ReturnsOriginal() {
        String phone = "12345";
        String masked = SensitiveDataMasker.maskPhone(phone);
        assertEquals(phone, masked);
    }

    @Test
    void maskPhone_WithNull_ReturnsNull() {
        String masked = SensitiveDataMasker.maskPhone(null);
        assertNull(masked);
    }

    @Test
    void maskIdCard_WithValidIdCard_ReturnsPartiallyMasked() {
        String idCard = "110101199001011234";
        String masked = SensitiveDataMasker.maskIdCard(idCard);
        assertEquals("110101********1234", masked);
    }

    @Test
    void maskIdCard_WithInvalidIdCard_ReturnsOriginal() {
        String idCard = "12345";
        String masked = SensitiveDataMasker.maskIdCard(idCard);
        assertEquals(idCard, masked);
    }

    @Test
    void maskBankCard_WithValidBankCard_ReturnsPartiallyMasked() {
        String bankCard = "6222021234567890";
        String masked = SensitiveDataMasker.maskBankCard(bankCard);
        assertEquals("6222****7890", masked);
    }

    @Test
    void maskBankCard_WithShortCard_ReturnsOriginal() {
        String bankCard = "1234567";
        String masked = SensitiveDataMasker.maskBankCard(bankCard);
        assertEquals(bankCard, masked);
    }

    @Test
    void maskEmail_WithValidEmail_ReturnsPartiallyMasked() {
        String email = "user@example.com";
        String masked = SensitiveDataMasker.maskEmail(email);
        assertEquals("use***@example.com", masked);
    }

    @Test
    void maskEmail_WithInvalidEmail_ReturnsOriginal() {
        String email = "notanemail";
        String masked = SensitiveDataMasker.maskEmail(email);
        assertEquals(email, masked);
    }

    @Test
    void maskPassword_ReturnsFullyMasked() {
        String password = "mySecretPassword123";
        String masked = SensitiveDataMasker.maskPassword(password);
        assertEquals("******", masked);
    }

    @Test
    void maskPassword_WithNull_ReturnsNull() {
        String masked = SensitiveDataMasker.maskPassword(null);
        assertNull(masked);
    }

    @Test
    void mask_WithValidString_ReturnsPartiallyMasked() {
        String str = "1234567890";
        String masked = SensitiveDataMasker.mask(str, 3, 3);
        assertEquals("123****890", masked);
    }

    @Test
    void mask_WithShortString_ReturnsOriginal() {
        String str = "12345";
        String masked = SensitiveDataMasker.mask(str, 3, 3);
        assertEquals(str, masked);
    }
}
