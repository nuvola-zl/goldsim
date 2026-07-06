package com.gold.mallApi.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BuyInfoVO {
    private String goodsName;
    private String category;
    private BigDecimal weight;
    private String coverImage;
    private Integer quantity;

    // ==================== 【核心改动】实时计价相关 ====================
    // unitPrice: 活动价优先，否则是实时金价计算出的单价
    // totalPrice: unitPrice * quantity
    // priceType: 0-实时计价  1-活动价，方便前端展示"限时抢购"标签
    // =================================================================
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer priceType;

    // 可售库存（stock - frozen_stock），前端用于判断能否购买
    private Integer availableStock;

    // 活动信息（只有进行中的活动才会返回，购买页用）
    private ActivityInfo activityInfo;

    @Data
    public static class ActivityInfo {
        private Long activityId;
        private String activityName;
        private BigDecimal activityPrice;
        private BigDecimal originalPrice;
        private Integer limitPerUser;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer status; // 购买页这里只可能是 1:进行中
    }
}