package com.gold.order.domain.dto;

import lombok.Data;

/**
 * 创建订单入参DTO
 */
@Data
public class OrderCreateDTO {
    /** 商品规格id */
    private Long specId;
    /** 购买数量 */
    private Integer buyNum;
    /** 自提门店id */
    private Long storeId;
    /** 下单备注 */
    private String remark;
    /**
     * 幂等键，前端生成唯一ID（如 UUID）
     * 同一 requestId 只能创建一个订单，防止重复提交
     */
    private String requestId;
}