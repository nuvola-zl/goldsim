package com.gold.product.entity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityGoodsInfo {
    private Long activityId;
    private String activityName;
    private Long specId;
    private BigDecimal originalPrice;
    private BigDecimal activityPrice;
    private Integer activityStock;
    private Integer soldCount;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}