package com.gold.api.domain.dto;

import lombok.Data;

@Data
public class DeductStockRequest {
    /** 商品规格ID */
    private Long specId;
    /** 扣减数量 */
    private Integer quantity;

    private String orderNo;  // 新增：用于幂等
}