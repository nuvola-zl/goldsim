package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交易统计概况VO
 */
@Data
@Builder
public class TradeStatisticsVO {
    private Integer totalTradeCount;       // 总交易笔数
    private Integer buyCount;              // 买入笔数
    private Integer sellCount;             // 卖出笔数
    private BigDecimal totalBuyValue;      // 累计买入金额
    private BigDecimal totalSellValue;     // 累计卖出金额
    private BigDecimal totalFee;           // 累计手续费
    private BigDecimal totalRealizedProfit; // 累计已实现盈亏
    private BigDecimal avgProfitPerTrade;  // 平均每笔盈亏
    private String winRate;              // 胜率，如 "66.67%"
}
