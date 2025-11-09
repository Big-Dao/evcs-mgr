package com.evcs.station.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.station.service.IStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 充电站分析控制器
 * 提供充电站数据分析、趋势预测和智能推荐功能
 */
@Slf4j
@Tag(name = "充电站分析", description = "充电站数据分析、趋势预测和智能推荐")
@RestController
@RequestMapping("/station/analytics")
@RequiredArgsConstructor
public class StationAnalyticsController {

    private final IStationService stationService;

    /**
     * 获取充电站分布热力图数据
     */
    @Operation(summary = "获取充电站分布热力图", description = "按区域统计充电站分布，用于热力图展示")
    @GetMapping("/heatmap")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<List<HeatmapData>> getStationHeatmap(
            @Parameter(description = "省份") @RequestParam(required = false) String province,
            @Parameter(description = "城市") @RequestParam(required = false) String city) {

        try {
            List<HeatmapData> heatmapData = stationService.getStationHeatmapData(province, city);
            return Result.success(heatmapData);
        } catch (Exception e) {
            log.error("获取充电站热力图数据失败", e);
            return Result.fail("获取热力图数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取充电站利用率统计
     */
    @Operation(summary = "获取利用率统计", description = "获取指定时间范围内的充电站利用率统计")
    @GetMapping("/utilization")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<UtilizationStatistics> getUtilizationStatistics(
            @Parameter(description = "开始时间") @RequestParam LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam LocalDateTime endTime,
            @Parameter(description = "充电站ID") @RequestParam(required = false) Long stationId) {

        try {
            UtilizationStatistics statistics = stationService.getUtilizationStatistics(startTime, endTime, stationId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取利用率统计失败", e);
            return Result.fail("获取利用率统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取收入趋势分析
     */
    @Operation(summary = "获取收入趋势", description = "获取指定时间范围内的充电站收入趋势分析")
    @GetMapping("/revenue-trend")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:revenue')")
    @DataScope
    public Result<RevenueTrend> getRevenueTrend(
            @Parameter(description = "开始时间") @RequestParam LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam LocalDateTime endTime,
            @Parameter(description = "时间粒度: day/week/month") @RequestParam(defaultValue = "day") String granularity,
            @Parameter(description = "充电站ID") @RequestParam(required = false) Long stationId) {

        try {
            RevenueTrend trend = stationService.getRevenueTrend(startTime, endTime, granularity, stationId);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取收入趋势失败", e);
            return Result.fail("获取收入趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取站点推荐位置
     */
    @Operation(summary = "获取站点推荐", description = "基于数据分析推荐新的充电站建设位置")
    @GetMapping("/recommendations")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<List<StationRecommendation>> getStationRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "5") Integer limit) {

        try {
            List<StationRecommendation> recommendations = stationService.getStationRecommendations(limit);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("获取站点推荐失败", e);
            return Result.fail("获取站点推荐失败: " + e.getMessage());
        }
    }

    /**
     * 获取充电站绩效排行
     */
    @Operation(summary = "获取绩效排行", description = "获取充电站绩效排行榜，支持多种排序方式")
    @GetMapping("/ranking")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<IPage<StationRanking>> getStationRanking(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Long size,
            @Parameter(description = "排序方式: revenue/utilization/chargers") @RequestParam(defaultValue = "revenue") String sortBy,
            @Parameter(description = "时间范围: 7d/30d/90d") @RequestParam(defaultValue = "30d") String timeRange) {

        try {
            Page<StationRanking> page = new Page<>(current, size);
            IPage<StationRanking> ranking = stationService.getStationRanking(page, sortBy, timeRange);
            return Result.success(ranking);
        } catch (Exception e) {
            log.error("获取绩效排行失败", e);
            return Result.fail("获取绩效排行失败: " + e.getMessage());
        }
    }

    /**
     * 获取区域分析报告
     */
    @Operation(summary = "获取区域分析", description = "获取指定区域的充电站市场分析报告")
    @GetMapping("/regional-analysis")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<RegionalAnalysis> getRegionalAnalysis(
            @Parameter(description = "省份") @RequestParam String province,
            @Parameter(description = "城市") @RequestParam(required = false) String city) {

        try {
            RegionalAnalysis analysis = stationService.getRegionalAnalysis(province, city);
            return Result.success(analysis);
        } catch (Exception e) {
            log.error("获取区域分析失败", e);
            return Result.fail("获取区域分析失败: " + e.getMessage());
        }
    }

    /**
     * 热力图数据
     */
    public static class HeatmapData {
        private Double latitude;
        private Double longitude;
        private Integer weight;
        private String areaName;
        private Integer stationCount;
        private Integer chargerCount;

        // Getters and setters
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Integer getWeight() { return weight; }
        public void setWeight(Integer weight) { this.weight = weight; }
        public String getAreaName() { return areaName; }
        public void setAreaName(String areaName) { this.areaName = areaName; }
        public Integer getStationCount() { return stationCount; }
        public void setStationCount(Integer stationCount) { this.stationCount = stationCount; }
        public Integer getChargerCount() { return chargerCount; }
        public void setChargerCount(Integer chargerCount) { this.chargerCount = chargerCount; }
    }

    /**
     * 利用率统计
     */
    public static class UtilizationStatistics {
        private Double averageUtilizationRate;
        private Double peakUtilizationRate;
        private Double lowUtilizationRate;
        private Map<String, Double> hourlyUtilization;
        private Map<String, Double> weeklyUtilization;
        private List<StationUtilization> stationDetails;

        // Getters and setters
        public Double getAverageUtilizationRate() { return averageUtilizationRate; }
        public void setAverageUtilizationRate(Double averageUtilizationRate) { this.averageUtilizationRate = averageUtilizationRate; }
        public Double getPeakUtilizationRate() { return peakUtilizationRate; }
        public void setPeakUtilizationRate(Double peakUtilizationRate) { this.peakUtilizationRate = peakUtilizationRate; }
        public Double getLowUtilizationRate() { return lowUtilizationRate; }
        public void setLowUtilizationRate(Double lowUtilizationRate) { this.lowUtilizationRate = lowUtilizationRate; }
        public Map<String, Double> getHourlyUtilization() { return hourlyUtilization; }
        public void setHourlyUtilization(Map<String, Double> hourlyUtilization) { this.hourlyUtilization = hourlyUtilization; }
        public Map<String, Double> getWeeklyUtilization() { return weeklyUtilization; }
        public void setWeeklyUtilization(Map<String, Double> weeklyUtilization) { this.weeklyUtilization = weeklyUtilization; }
        public List<StationUtilization> getStationDetails() { return stationDetails; }
        public void setStationDetails(List<StationUtilization> stationDetails) { this.stationDetails = stationDetails; }

        public static class StationUtilization {
            private Long stationId;
            private String stationName;
            private Double utilizationRate;
            private Integer totalChargers;
            private Integer averageChargingTime;

            // Getters and setters
            public Long getStationId() { return stationId; }
            public void setStationId(Long stationId) { this.stationId = stationId; }
            public String getStationName() { return stationName; }
            public void setStationName(String stationName) { this.stationName = stationName; }
            public Double getUtilizationRate() { return utilizationRate; }
            public void setUtilizationRate(Double utilizationRate) { this.utilizationRate = utilizationRate; }
            public Integer getTotalChargers() { return totalChargers; }
            public void setTotalChargers(Integer totalChargers) { this.totalChargers = totalChargers; }
            public Integer getAverageChargingTime() { return averageChargingTime; }
            public void setAverageChargingTime(Integer averageChargingTime) { this.averageChargingTime = averageChargingTime; }
        }
    }

    /**
     * 收入趋势数据
     */
    public static class RevenueTrend {
        private Double totalRevenue;
        private Double averageDailyRevenue;
        private Double growthRate;
        private List<TrendPoint> trendPoints;

        // Getters and setters
        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public Double getAverageDailyRevenue() { return averageDailyRevenue; }
        public void setAverageDailyRevenue(Double averageDailyRevenue) { this.averageDailyRevenue = averageDailyRevenue; }
        public Double getGrowthRate() { return growthRate; }
        public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }
        public List<TrendPoint> getTrendPoints() { return trendPoints; }
        public void setTrendPoints(List<TrendPoint> trendPoints) { this.trendPoints = trendPoints; }

        public static class TrendPoint {
            private String date;
            private Double revenue;
            private Integer transactionCount;

            // Getters and setters
            public String getDate() { return date; }
            public void setDate(String date) { this.date = date; }
            public Double getRevenue() { return revenue; }
            public void setRevenue(Double revenue) { this.revenue = revenue; }
            public Integer getTransactionCount() { return transactionCount; }
            public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
        }
    }

    /**
     * 站点推荐
     */
    public static class StationRecommendation {
        private String address;
        private Double latitude;
        private Double longitude;
        private String recommendationReason;
        private Double expectedRevenue;
        private Integer recommendedChargers;
        private Double competitionScore;
        private Double demandScore;

        // Getters and setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public String getRecommendationReason() { return recommendationReason; }
        public void setRecommendationReason(String recommendationReason) { this.recommendationReason = recommendationReason; }
        public Double getExpectedRevenue() { return expectedRevenue; }
        public void setExpectedRevenue(Double expectedRevenue) { this.expectedRevenue = expectedRevenue; }
        public Integer getRecommendedChargers() { return recommendedChargers; }
        public void setRecommendedChargers(Integer recommendedChargers) { this.recommendedChargers = recommendedChargers; }
        public Double getCompetitionScore() { return competitionScore; }
        public void setCompetitionScore(Double competitionScore) { this.competitionScore = competitionScore; }
        public Double getDemandScore() { return demandScore; }
        public void setDemandScore(Double demandScore) { this.demandScore = demandScore; }
    }

    /**
     * 充电站排行
     */
    public static class StationRanking {
        private Long stationId;
        private String stationName;
        private String address;
        private Double score;
        private String rankChange;
        private Map<String, Object> metrics;

        // Getters and setters
        public Long getStationId() { return stationId; }
        public void setStationId(Long stationId) { this.stationId = stationId; }
        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getRankChange() { return rankChange; }
        public void setRankChange(String rankChange) { this.rankChange = rankChange; }
        public Map<String, Object> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    }

    /**
     * 区域分析报告
     */
    public static class RegionalAnalysis {
        private String regionName;
        private Integer totalStations;
        private Integer totalChargers;
        private Double marketPenetration;
        private Double averageUtilization;
        private List<MarketOpportunity> opportunities;
        private List<CompetitorInfo> competitors;

        // Getters and setters
        public String getRegionName() { return regionName; }
        public void setRegionName(String regionName) { this.regionName = regionName; }
        public Integer getTotalStations() { return totalStations; }
        public void setTotalStations(Integer totalStations) { this.totalStations = totalStations; }
        public Integer getTotalChargers() { return totalChargers; }
        public void setTotalChargers(Integer totalChargers) { this.totalChargers = totalChargers; }
        public Double getMarketPenetration() { return marketPenetration; }
        public void setMarketPenetration(Double marketPenetration) { this.marketPenetration = marketPenetration; }
        public Double getAverageUtilization() { return averageUtilization; }
        public void setAverageUtilization(Double averageUtilization) { this.averageUtilization = averageUtilization; }
        public List<MarketOpportunity> getOpportunities() { return opportunities; }
        public void setOpportunities(List<MarketOpportunity> opportunities) { this.opportunities = opportunities; }
        public List<CompetitorInfo> getCompetitors() { return competitors; }
        public void setCompetitors(List<CompetitorInfo> competitors) { this.competitors = competitors; }

        public static class MarketOpportunity {
            private String type;
            private String description;
            private Double potentialValue;
            private Integer priority;

            // Getters and setters
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public Double getPotentialValue() { return potentialValue; }
            public void setPotentialValue(Double potentialValue) { this.potentialValue = potentialValue; }
            public Integer getPriority() { return priority; }
            public void setPriority(Integer priority) { this.priority = priority; }
        }

        public static class CompetitorInfo {
            private String name;
            private Integer stationCount;
            private Double marketShare;
            private String strength;

            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public Integer getStationCount() { return stationCount; }
            public void setStationCount(Integer stationCount) { this.stationCount = stationCount; }
            public Double getMarketShare() { return marketShare; }
            public void setMarketShare(Double marketShare) { this.marketShare = marketShare; }
            public String getStrength() { return strength; }
            public void setStrength(String strength) { this.strength = strength; }
        }
    }
}