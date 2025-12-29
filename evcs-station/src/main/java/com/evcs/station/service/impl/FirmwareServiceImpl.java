package com.evcs.station.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.station.entity.Firmware;
import com.evcs.station.mapper.FirmwareMapper;
import com.evcs.station.service.FirmwareService;
import org.springframework.stereotype.Service;

@Service
public class FirmwareServiceImpl extends ServiceImpl<FirmwareMapper, Firmware> implements FirmwareService {
}
