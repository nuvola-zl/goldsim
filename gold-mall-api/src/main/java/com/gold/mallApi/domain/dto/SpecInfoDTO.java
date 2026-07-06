package com.gold.mallApi.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpecInfoDTO {
    private Long goodsId;
    private String goodsName;
    private String category;
    private String metalType;
    private Long storeId;

    private Long specId;
    private BigDecimal weight;
    private BigDecimal craftFee;

    // ==================== 【核心改动】价格字段明确化 ====================
    // referenceXxx: 管理端录入时的展示参考价，仅供参考，下单请勿直接使用
    // realTimeXxx:  下单时的实时金价和总价，无活动时请用这个
    // activityXxx:  如果有进行中活动，优先使用活动价，不再使用实时总价
    // =================================================================
    private BigDecimal referencePricePerGram;
    private BigDecimal referenceTotalPrice;

    private BigDecimal realTimePricePerGram;
    private BigDecimal realTimeTotalPrice;

    // 活动信息（如果有进行中活动，订单服务请直接使用 activityPrice）
    private Long activityId;
    private String activityName;
    private BigDecimal activityPrice;
    private BigDecimal originalPrice;
    private Integer activityStock;
    private Integer limitPerUser;

    private Integer stock;
    private Integer frozenStock;
    private Integer availableStock;    // 可售库存
    private String coverImage;
    private Integer saleStatus;
}