package com.evcs.auth.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PageResponse<T> {
    List<T> records;
    long total;
    long size;
    long current;
    long pages;
}

