package com.evcs.common.page;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 分页查询基类
 */
@Data
public class PageQuery {
    
    /**
     * 页码，从1开始
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private Integer size = 10;
    
    /**
     * 排序字段
     */
    private String sortBy;
    
    /**
     * 排序方向：asc/desc
     */
    private String sortDir = "desc";
    
    /**
     * 获取偏移量
     */
    public Integer getOffset() {
        return (page - 1) * size;
    }
}