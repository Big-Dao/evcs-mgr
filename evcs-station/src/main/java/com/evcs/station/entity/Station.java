package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 充电站实体
 */
@Data
@TableName("charging_station")
public class Station {
    @TableId(value = "station_id", type = IdType.AUTO)
    private Long stationId;
    private Long tenantId;
    private String stationCode;
    private String stationName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer status; // 1-启用 0-停用
    private String province;
    private String city;
    private String district;
    
    // 统计字段 - 通过 JOIN 查询计算，不存储在表中
    @TableField(exist = false)
    private Integer totalChargers;
    @TableField(exist = false)
    private Integer availableChargers;
    @TableField(exist = false)
    private Integer chargingChargers;
    @TableField(exist = false)
    private Integer faultChargers;
    
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
