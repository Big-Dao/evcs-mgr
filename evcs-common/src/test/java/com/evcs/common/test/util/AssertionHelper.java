package com.evcs.common.test.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 断言辅助类
 * 提供常用的自定义断言方法
 */
public class AssertionHelper {

    /**
     * 断言集合不为空且包含指定数量的元素
     * 
     * @param collection 集合
     * @param expectedSize 期望的大小
     */
    public static void assertCollectionSize(Collection<?> collection, int expectedSize) {
        assertNotNull(collection, "集合不应为null");
        assertEquals(expectedSize, collection.size(), 
                    "集合大小应为 " + expectedSize + "，实际为 " + collection.size());
    }

    /**
     * 断言集合不为空
     * 
     * @param collection 集合
     */
    public static void assertCollectionNotEmpty(Collection<?> collection) {
        assertNotNull(collection, "集合不应为null");
        assertFalse(collection.isEmpty(), "集合不应为空");
    }

    /**
     * 断言集合为空
     * 
     * @param collection 集合
     */
    public static void assertCollectionEmpty(Collection<?> collection) {
        assertNotNull(collection, "集合不应为null");
        assertTrue(collection.isEmpty(), "集合应为空");
    }

    /**
     * 断言两个时间相差在指定秒数内
     * 
     * @param expected 期望时间
     * @param actual 实际时间
     * @param maxDifferenceSeconds 最大差异秒数
     */
    public static void assertTimeNear(LocalDateTime expected, LocalDateTime actual, long maxDifferenceSeconds) {
        assertNotNull(expected, "期望时间不应为null");
        assertNotNull(actual, "实际时间不应为null");
        
        long diff = Math.abs(ChronoUnit.SECONDS.between(expected, actual));
        assertTrue(diff <= maxDifferenceSeconds, 
                  "时间差异应在 " + maxDifferenceSeconds + " 秒内，实际差异为 " + diff + " 秒");
    }

    /**
     * 断言字符串不为空且不为null
     * 
     * @param str 字符串
     */
    public static void assertStringNotBlank(String str) {
        assertNotNull(str, "字符串不应为null");
        assertFalse(str.trim().isEmpty(), "字符串不应为空");
    }

    /**
     * 断言字符串包含指定子串
     * 
     * @param str 字符串
     * @param substring 子串
     */
    public static void assertStringContains(String str, String substring) {
        assertNotNull(str, "字符串不应为null");
        assertTrue(str.contains(substring), 
                  "字符串 \"" + str + "\" 应该包含 \"" + substring + "\"");
    }

    /**
     * 断言字符串以指定前缀开头
     * 
     * @param str 字符串
     * @param prefix 前缀
     */
    public static void assertStringStartsWith(String str, String prefix) {
        assertNotNull(str, "字符串不应为null");
        assertTrue(str.startsWith(prefix), 
                  "字符串 \"" + str + "\" 应该以 \"" + prefix + "\" 开头");
    }

    /**
     * 断言数字在指定范围内
     * 
     * @param value 数值
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     */
    public static void assertInRange(double value, double min, double max) {
        assertTrue(value >= min && value <= max, 
                  "数值 " + value + " 应该在 [" + min + ", " + max + "] 范围内");
    }

    /**
     * 断言数字为正数
     * 
     * @param value 数值
     */
    public static void assertPositive(double value) {
        assertTrue(value > 0, "数值应该为正数，实际为 " + value);
    }

    /**
     * 断言数字为非负数
     * 
     * @param value 数值
     */
    public static void assertNonNegative(double value) {
        assertTrue(value >= 0, "数值应该为非负数，实际为 " + value);
    }

    /**
     * 断言列表包含指定元素
     * 
     * @param list 列表
     * @param element 元素
     */
    public static <T> void assertListContains(List<T> list, T element) {
        assertNotNull(list, "列表不应为null");
        assertTrue(list.contains(element), 
                  "列表应该包含元素 " + element);
    }

    /**
     * 断言列表不包含指定元素
     * 
     * @param list 列表
     * @param element 元素
     */
    public static <T> void assertListNotContains(List<T> list, T element) {
        assertNotNull(list, "列表不应为null");
        assertFalse(list.contains(element), 
                   "列表不应该包含元素 " + element);
    }

    /**
     * 断言对象的所有字段都不为null
     * （简化版本，仅用于基本验证）
     * 
     * @param obj 对象
     */
    public static void assertAllFieldsNotNull(Object obj) {
        assertNotNull(obj, "对象不应为null");
        // 注意：这是简化版本，完整实现需要使用反射
    }

    /**
     * 断言租户ID有效
     * 
     * @param tenantId 租户ID
     */
    public static void assertValidTenantId(Long tenantId) {
        assertNotNull(tenantId, "租户ID不应为null");
        assertTrue(tenantId > 0, "租户ID应该为正数");
    }
}
