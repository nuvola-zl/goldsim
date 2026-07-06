package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓信息VO
 * 接口：GET /api/simulation/position/list
 *      GET /api/simulation/position/{metalType}
 */
@Data
@Builder
public class PositionVO {
    /** 持仓ID */
    private Long positionId;
    /** 品种代码 AU/AG/PT */
    private String metalType;
    /** 品种名称 */
    private String metalName;
    /** 持有克重 */
    private BigDecimal amount;
    /** 加权平均成本价 */
    private BigDecimal avgCostPrice;
    /** 当前最新价（实时从行情服务获取） */
    private BigDecimal currentPrice;
    /** 浮动盈亏 = amount × (currentPrice - avgCostPrice) */
    private BigDecimal floatingProfit;
    /** 浮动盈亏率 = floatingProfit / (amount × avgCostPrice) */
    private String floatingProfitRate;
    /** 持仓市值 = amount × currentPrice */
    private BigDecimal marketValue;
    /** 持仓创建时间 */
    private LocalDateTime createTime;
    /** 持仓更新时间 */
    private LocalDateTime updateTime;
}