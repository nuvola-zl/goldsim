package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易详情VO（单笔）
 */
@Data
@Builder
public class TradeRecordDetailVO {
    private String requestId;           // 请求ID
    private String tradeType;           // BUY / SELL
    private String tradeTypeName;       // 买入 / 卖出
    private String metalType;           // AU / AG / PT
    private String metalName;           // 黄金/白银/铂金
    private BigDecimal amount;          // 交易克重
    private BigDecimal price;           // 成交单价
    private BigDecimal tradeValue;      // 成交金额
    private BigDecimal fee;             // 手续费
    private BigDecimal realizedProfit;  // 已实现盈亏
    private BigDecimal balanceBefore;   // 交易前余额
    private BigDecimal balanceAfter;    // 交易后余额
    private LocalDateTime createTime;   // 成交时间
}