package com.evcs.station.client;

import com.evcs.common.result.Result;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "evcs-protocol", contextId = "protocolClient", path = "/protocol/command")
public interface ProtocolClient {

    @PostMapping("/start")
    Result<ProtocolResponse> startCharging(@RequestBody ProtocolRequest request);

    @PostMapping("/stop")
    Result<ProtocolResponse> stopCharging(@RequestBody ProtocolRequest request);

    @PostMapping("/reserve")
    Result<ProtocolResponse> reserveNow(@RequestBody ProtocolRequest request);

    @PostMapping("/cancel-reservation")
    Result<ProtocolResponse> cancelReservation(@RequestBody ProtocolRequest request);

    @PostMapping("/update-firmware")
    Result<ProtocolResponse> updateFirmware(@RequestBody ProtocolRequest request);

    @PostMapping("/set-charging-profile")
    Result<ProtocolResponse> setChargingProfile(@RequestBody ProtocolRequest request);
}
