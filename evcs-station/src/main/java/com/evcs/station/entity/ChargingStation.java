package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 充电站实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("charging_station")
public class ChargingStation extends BaseEntity {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 充电站编码
     */
    private String stationCode;
    
    /**
     * 充电站名称
     */
    private String stationName;
    
    /**
     * 运营商ID（关联租户）
     */
    private Long operatorId;
    
    /**
     * 运营商名称
     */
    private String operatorName;
    
    /**
     * 充电站类型：1-直流快充，2-交流慢充，3-混合站
     */
    private Integer stationType;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 区县
     */
    private String district;
    
    /**
     * 详细地址
     */
    private String address;
    
    /**
     * 经度
     */
    private BigDecimal longitude;
    
    /**
     * 纬度
     */
    private BigDecimal latitude;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 服务时间
     */
    private String serviceTime;
    
    /**
     * 停车费信息
     */
    private String parkingFee;
    
    /**
     * 支付方式：使用PostgreSQL的数组类型存储多种支付方式
     * 例如：{1,2,3} 表示支持现金、微信、支付宝
     */
    private String paymentMethods;
    
    /**
     * 充电桩总数
     */
    private Integer totalChargers;
    
    /**
     * 可用充电桩数
     */
    private Integer availableChargers;
    
    /**
     * 故障充电桩数
     */
    private Integer faultyChargers;
    
    /**
     * 充电中充电桩数
     */
    private Integer chargingChargers;
    
    /**
     * 站点状态：0-建设中，1-运营中，2-维护中，3-停用
     */
    private Integer status;
    
    /**
     * 是否支持预约：0-否，1-是
     */
    private Integer supportReservation;
    
    /**
     * 网络类型：1-有线，2-4G，3-5G，4-WiFi
     */
    private Integer networkType;
    
    /**
     * 最后心跳时间
     */
    private java.time.LocalDateTime lastHeartbeat;
    
    /**
     * 站点图片URLs（JSON数组格式）
     */
    private String imageUrls;
    
    /**
     * 设施说明（使用PostgreSQL的JSONB类型存储结构化数据）
     * 例如：{"restroom": true, "restaurant": false, "supermarket": true}
     */
    private String facilities;
    
    /**
     * 备注
     */
    private String remark;
}