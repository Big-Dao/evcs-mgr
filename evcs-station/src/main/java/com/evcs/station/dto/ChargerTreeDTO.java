package com.evcs.station.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ChargerTreeDTO {

    private Long chargerId;
    private String chargerCode;
    private String chargerName;

    private Integer status;
    private Integer enabled;

    private Integer gunCount;

    private List<ConnectorTreeDTO> connectors = new ArrayList<>();
}
