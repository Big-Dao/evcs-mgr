package com.evcs.common.util;

/**
 * 地理坐标校验工具
 */
public class GeoValidator {

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    /**
     * 校验纬度是否有效
     *
     * @param latitude 纬度
     * @return true 有效, false 无效
     */
    public static boolean isValidLatitude(Double latitude) {
        return latitude != null && latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
    }

    /**
     * 校验经度是否有效
     *
     * @param longitude 经度
     * @return true 有效, false 无效
     */
    public static boolean isValidLongitude(Double longitude) {
        return longitude != null && longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }

    /**
     * 校验经纬度坐标
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @throws IllegalArgumentException 如果坐标无效
     */
    public static void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("经纬度不能为空");
        }
        if (!isValidLatitude(latitude)) {
            throw new IllegalArgumentException(
                    String.format("无效的纬度值: %.6f (有效范围: %.1f ~ %.1f)",
                            latitude, MIN_LATITUDE, MAX_LATITUDE));
        }
        if (!isValidLongitude(longitude)) {
            throw new IllegalArgumentException(
                    String.format("无效的经度值: %.6f (有效范围: %.1f ~ %.1f)",
                            longitude, MIN_LONGITUDE, MAX_LONGITUDE));
        }
    }
}
