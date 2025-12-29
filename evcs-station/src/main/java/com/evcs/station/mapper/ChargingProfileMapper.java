package com.evcs.station.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.station.entity.ChargingProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChargingProfileMapper extends BaseMapper<ChargingProfile> {
}
