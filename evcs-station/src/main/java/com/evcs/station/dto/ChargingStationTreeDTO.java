package com.evcs.station.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 充电站-充电桩-枪口树形结构 DTO
 */
@Data
public class ChargingStationTreeDTO {

    private Long stationId;
    private String stationCode;
    private String stationName;
    private Integer status;

    private List<ChargerTreeDTO> chargers = new ArrayList<>();
}
