package com.evcs.common.page;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 总页数
     */
    private Integer pages;
    
    public PageResult() {}
    
    public PageResult(List<T> records, Long total, Integer page, Integer size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }
    
    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0L, 1, 10);
    }
    
    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        return new PageResult<>(records, total, page, size);
    }
}