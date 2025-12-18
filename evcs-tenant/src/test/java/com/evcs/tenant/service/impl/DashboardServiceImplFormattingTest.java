package com.evcs.tenant.service.impl;

import com.evcs.common.test.base.BaseServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Dashboard 最近订单字段格式化")
class DashboardServiceImplFormattingTest extends BaseServiceTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("create_time 格式化 - LocalDateTime 时应正确输出")
    void testFormatCreateTime_shouldFormat_whenLocalDateTime() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2025, 12, 18, 10, 11, 12);

        // Act
        String formatted = DashboardServiceImpl.formatCreateTime(dateTime, FORMATTER);

        // Assert
        assertEquals("2025-12-18 10:11:12", formatted, "LocalDateTime 应按 yyyy-MM-dd HH:mm:ss 格式化");
    }

    @Test
    @DisplayName("create_time 格式化 - Timestamp 时应正确输出")
    void testFormatCreateTime_shouldFormat_whenTimestamp() {
        // Arrange
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(2025, 12, 18, 10, 11, 12));

        // Act
        String formatted = DashboardServiceImpl.formatCreateTime(timestamp, FORMATTER);

        // Assert
        assertEquals("2025-12-18 10:11:12", formatted, "Timestamp 应按 yyyy-MM-dd HH:mm:ss 格式化");
    }

    @Test
    @DisplayName("create_time 格式化 - Date 时应正确输出")
    void testFormatCreateTime_shouldFormat_whenDate() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2025, 12, 18, 10, 11, 12);
        Date date = new Date(Timestamp.valueOf(dateTime).getTime());

        // Act
        String formatted = DashboardServiceImpl.formatCreateTime(date, FORMATTER);

        // Assert
        assertEquals("2025-12-18 10:11:12", formatted, "Date 应按 yyyy-MM-dd HH:mm:ss 格式化");
    }

    @Test
    @DisplayName("create_time 格式化 - null 时应返回空字符串")
    void testFormatCreateTime_shouldReturnEmpty_whenNull() {
        // Arrange
        Object createTime = null;

        // Act
        String formatted = DashboardServiceImpl.formatCreateTime(createTime, FORMATTER);

        // Assert
        assertEquals("", formatted, "create_time 为 null 时应返回空字符串");
    }

    @Test
    @DisplayName("amount 转换 - 多类型输入应安全转换为 BigDecimal")
    void testToBigDecimal_shouldConvert_whenMultipleTypes() {
        // Arrange
        BigDecimal bigDecimal = new BigDecimal("12.34");

        // Act
        BigDecimal fromBd = DashboardServiceImpl.toBigDecimal(bigDecimal);
        BigDecimal fromLong = DashboardServiceImpl.toBigDecimal(12L);
        BigDecimal fromInteger = DashboardServiceImpl.toBigDecimal(34);
        BigDecimal fromString = DashboardServiceImpl.toBigDecimal("56.78");
        BigDecimal fromNull = DashboardServiceImpl.toBigDecimal(null);
        BigDecimal fromInvalid = DashboardServiceImpl.toBigDecimal("not-a-number");

        // Assert
        assertEquals(new BigDecimal("12.34"), fromBd, "BigDecimal 输入应保持不变");
        assertEquals(new BigDecimal("12"), fromLong, "Long 输入应转换为 BigDecimal");
        assertEquals(new BigDecimal("34"), fromInteger, "Integer 输入应转换为 BigDecimal");
        assertEquals(new BigDecimal("56.78"), fromString, "String 数字输入应转换为 BigDecimal");
        assertEquals(BigDecimal.ZERO, fromNull, "null 输入应转换为 0");
        assertEquals(BigDecimal.ZERO, fromInvalid, "非法数字输入应回退为 0");
    }

    @Test
    @DisplayName("String 安全转换 - null 时应返回空字符串")
    void testToStringOrEmpty_shouldReturnEmpty_whenNull() {
        // Arrange
        Object value = null;

        // Act
        String actual = DashboardServiceImpl.toStringOrEmpty(value);

        // Assert
        assertEquals("", actual, "null 应转换为空字符串");
    }

    @Test
    @DisplayName("String 安全转换 - 非 null 时应返回 toString")
    void testToStringOrEmpty_shouldReturnToString_whenNotNull() {
        // Arrange
        Object value = 123;

        // Act
        String actual = DashboardServiceImpl.toStringOrEmpty(value);

        // Assert
        assertEquals("123", actual, "非 null 值应返回其字符串表示");
    }
}
