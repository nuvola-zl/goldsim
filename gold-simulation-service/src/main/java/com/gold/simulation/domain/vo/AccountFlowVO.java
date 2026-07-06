package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金流水VO
 */
@Data
@Builder
public class AccountFlowVO {
    private String requestId;           // 流水号（请求ID）
    private String tradeType;           // BUY / SELL
    private String metalType;           // AU / AG / PT
    private String metalName;           // 黄金/白银/铂金
    private BigDecimal amount;          // 交易克重
    private BigDecimal price;           // 成交单价
    private BigDecimal tradeValue;      // 成交金额
    private BigDecimal fee;             // 手续费
    private BigDecimal realizedProfit;  // 卖出已实现盈亏（买入为null）
    private BigDecimal balanceAfter;    // 变动后余额
    private LocalDateTime createTime;   // 交易时间
}
