package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 固件包实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("firmware")
public class Firmware extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 固件版本号
     */
    private String firmwareVersion;

    /**
     * 适用充电桩型号
     */
    private String model;

    /**
     * 固件下载地址
     */
    private String url;

    /**
     * 文件校验和 (MD5)
     */
    private String md5;

    /**
     * 版本描述
     */
    private String description;
}
