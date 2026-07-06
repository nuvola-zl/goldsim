package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 可卖出数量VO
 */
@Data
@Builder
public class PositionAvailableVO {
    private String metalType;       // AU / AG / PT
    private String metalName;       // 黄金/白银/铂金
    private BigDecimal availableAmount; // 可卖出克重
    private Boolean hasPosition;    // 是否有持仓
}
