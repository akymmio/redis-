package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * 滚动分页查询字段
 */
@Data
public class Scroll {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
