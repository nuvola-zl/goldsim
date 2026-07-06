package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录VO
 * 接口：GET /api/simulation/trade/history
 */
@Data
@Builder
public class TradeRecordVO {
    /** 流水ID */
    private Long id;
    /** 交易类型 BUY/SELL */
    private String tradeType;
    /** 交易类型中文 */
    private String tradeTypeName;
    /** 品种代码 */
    private String metalType;
    /** 品种名称 */
    private String metalName;
    /** 交易克重 */
    private BigDecimal amount;
    /** 成交单价 */
    private BigDecimal price;
    /** 成交金额 */
    private BigDecimal tradeValue;
    /** 手续费 */
    private BigDecimal fee;
    /** 卖出已实现盈亏（买入为null） */
    private BigDecimal realizedProfit;
    /** 交易前余额 */
    private BigDecimal balanceBefore;
    /** 交易后余额 */
    private BigDecimal balanceAfter;
    /** 交易时间 */
    private LocalDateTime createTime;

    private String requestId;  // ← 新增
}