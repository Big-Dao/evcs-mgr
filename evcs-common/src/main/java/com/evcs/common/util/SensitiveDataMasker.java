package com.evcs.common.util;

import java.util.regex.Pattern;

/**
 * 敏感信息脱敏工具类
 * 用于脱敏日志中的敏感数据
 */
public class SensitiveDataMasker {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)(\\d{4})(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})(\\d{8})(\\d{4})");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})(\\d+)(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([^@]{1,3})[^@]*@");

    /**
     * 手机号脱敏：138****1234
     * 
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$3");
    }

    /**
     * 身份证号脱敏：前6后4
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return idCard;
        }
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$3");
    }

    /**
     * 银行卡号脱敏：前4后4
     * 
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return BANK_CARD_PATTERN.matcher(bankCard).replaceAll("$1****$3");
    }

    /**
     * 邮箱脱敏：保留前3位和@后的内容
     * 
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***@");
    }

    /**
     * 密码脱敏：完全隐藏
     * 
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String maskPassword(String password) {
        if (password == null) {
            return null;
        }
        return "******";
    }

    /**
     * 通用脱敏方法：保留前后各显示的字符数
     * 
     * @param str 原始字符串
     * @param showStart 开始显示的字符数
     * @param showEnd 结尾显示的字符数
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int showStart, int showEnd) {
        if (str == null || str.length() <= showStart + showEnd) {
            return str;
        }
        
        String start = str.substring(0, showStart);
        String end = str.substring(str.length() - showEnd);
        int maskLength = str.length() - showStart - showEnd;
        
        StringBuilder masked = new StringBuilder(start);
        for (int i = 0; i < maskLength; i++) {
            masked.append('*');
        }
        masked.append(end);
        
        return masked.toString();
    }
}
