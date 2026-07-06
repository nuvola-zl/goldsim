package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 持仓盈亏实时分析VO
 */
@Data
@Builder
public class PositionProfitVO {
    private String metalType;              // AU / AG / PT
    private String metalName;              // 黄金/白银/铂金
    private BigDecimal amount;             // 持仓克重
    private BigDecimal avgCostPrice;       // 加权平均成本价
    private BigDecimal latestPrice;          // 实时市价（元/克）
    private BigDecimal marketValue;        // 当前市值 = amount × latestPrice
    private BigDecimal floatingProfit;     // 浮动盈亏 = amount × (latestPrice - avgCostPrice)
    private String floatingProfitRate;     // 浮动收益率，如 "+5.32%" / "-1.20%"
}