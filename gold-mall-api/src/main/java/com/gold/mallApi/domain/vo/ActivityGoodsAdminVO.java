package com.gold.mallApi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityGoodsAdminVO {
    private Long activityGoodsId;
    private Long specId;
    private Long goodsId;
    private String goodsName;   // 冗余商品名，方便展示
    private BigDecimal weight;  // 冗余克重
    private BigDecimal originalPrice;
    private BigDecimal activityPrice;
    private Integer activityStock;
    private Integer soldCount;
    private Integer limitPerUser;
    private LocalDateTime createTime;
}