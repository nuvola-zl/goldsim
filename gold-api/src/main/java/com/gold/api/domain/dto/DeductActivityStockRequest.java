package com.gold.api.domain.dto;

import lombok.Data;

@Data
public class DeductActivityStockRequest {
    private Long activityId;
    private Long specId;
    private Integer quantity;
}