package com.evcs.common.test.util;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试数据工厂
 * 提供创建测试数据的便捷方法
 */
public class TestDataFactory {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1000);

    /**
     * 生成唯一的测试ID
     * 
     * @return 唯一ID
     */
    public static Long generateId() {
        return ID_GENERATOR.incrementAndGet();
    }

    /**
     * 生成测试编码
     * 
     * @param prefix 前缀
     * @return 测试编码
     */
    public static String generateCode(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    /**
     * 生成测试名称
     * 
     * @param prefix 前缀
     * @return 测试名称
     */
    public static String generateName(String prefix) {
        return prefix + "_" + generateId();
    }

    /**
     * 生成测试邮箱
     * 
     * @param username 用户名
     * @return 测试邮箱
     */
    public static String generateEmail(String username) {
        return username + "@test.com";
    }

    /**
     * 生成测试手机号
     * 
     * @return 测试手机号
     */
    public static String generatePhone() {
        return "138" + String.format("%08d", generateId() % 100000000);
    }

    /**
     * 获取当前时间
     * 
     * @return 当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 生成测试地址
     * 
     * @param city 城市
     * @return 测试地址
     */
    public static String generateAddress(String city) {
        return city + "市测试路" + generateId() + "号";
    }

    /**
     * 生成随机布尔值
     * 
     * @return 随机布尔值
     */
    public static boolean randomBoolean() {
        return Math.random() > 0.5;
    }

    /**
     * 生成指定范围内的随机整数
     * 
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 随机整数
     */
    public static int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    /**
     * 生成指定范围内的随机双精度浮点数
     * 
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 随机双精度浮点数
     */
    public static double randomDouble(double min, double max) {
        return min + (Math.random() * (max - min));
    }

    /**
     * 重置ID生成器
     */
    public static void resetIdGenerator() {
        ID_GENERATOR.set(1000);
    }
}
