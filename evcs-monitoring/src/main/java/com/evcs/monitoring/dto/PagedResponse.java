package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PagedResponse<T> {
    List<T> records;
    long total;
}
