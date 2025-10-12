package com.evcs.common.validation;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 时间范围验证工具类
 * 统一时间校验逻辑
 * 
 * @author EVCS Team
 * @since M6 - Week 6 Code Quality Improvement
 */
public class TimeRangeValidator {
    
    /**
     * 验证时间范围是否有效（结束时间必须晚于开始时间）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true表示时间范围有效
     */
    public static boolean isValidTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        Objects.requireNonNull(startTime, "开始时间不能为空");
        Objects.requireNonNull(endTime, "结束时间不能为空");
        return endTime.isAfter(startTime);
    }
    
    /**
     * 验证时间范围是否有效，允许为null（两者都为null或都不为null且有效）
     * 
     * @param startTime 开始时间（可为null）
     * @param endTime 结束时间（可为null）
     * @return true表示时间范围有效或两者都为null
     */
    public static boolean isValidTimeRangeOrNull(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) {
            return true;
        }
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isAfter(startTime);
    }
    
    /**
     * 验证LocalTime范围是否有效（用于计费时段）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true表示时间范围有效
     */
    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        Objects.requireNonNull(startTime, "开始时间不能为空");
        Objects.requireNonNull(endTime, "结束时间不能为空");
        return endTime.isAfter(startTime);
    }
    
    /**
     * 验证时间字符串格式并解析为LocalTime
     * 
     * @param timeStr 时间字符串（格式：HH:mm 或 HH:mm:ss）
     * @return LocalTime对象
     * @throws IllegalArgumentException 如果格式无效
     */
    public static LocalTime parseTime(String timeStr) {
        Objects.requireNonNull(timeStr, "时间字符串不能为空");
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的时间格式：" + timeStr + "，期望格式为 HH:mm 或 HH:mm:ss", e);
        }
    }
    
    /**
     * 验证时间字符串范围是否有效
     * 
     * @param startTimeStr 开始时间字符串
     * @param endTimeStr 结束时间字符串
     * @return true表示时间范围有效
     */
    public static boolean isValidTimeRangeString(String startTimeStr, String endTimeStr) {
        try {
            LocalTime start = parseTime(startTimeStr);
            LocalTime end = parseTime(endTimeStr);
            return isValidTimeRange(start, end);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证时间是否在指定范围内
     * 
     * @param time 待验证的时间
     * @param rangeStart 范围开始时间
     * @param rangeEnd 范围结束时间
     * @return true表示在范围内
     */
    public static boolean isInRange(LocalDateTime time, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Objects.requireNonNull(time, "时间不能为空");
        Objects.requireNonNull(rangeStart, "范围开始时间不能为空");
        Objects.requireNonNull(rangeEnd, "范围结束时间不能为空");
        
        return !time.isBefore(rangeStart) && !time.isAfter(rangeEnd);
    }
    
    /**
     * 计算两个时间之间的分钟数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分钟数
     */
    public static long getMinutesBetween(LocalDateTime startTime, LocalDateTime endTime) {
        Objects.requireNonNull(startTime, "开始时间不能为空");
        Objects.requireNonNull(endTime, "结束时间不能为空");
        
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }
}
