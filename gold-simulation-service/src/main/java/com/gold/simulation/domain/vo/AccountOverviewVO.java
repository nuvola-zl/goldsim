package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产总览VO
 * 接口：GET /api/simulation/account/overview
 */
@Data
@Builder
public class AccountOverviewVO {
    /** 总资产 = 可用余额 + 持仓市值 */
    private BigDecimal totalAsset;
    /** 可用余额 */
    private BigDecimal balance;
    /** 持仓总市值 */
    private BigDecimal positionMarketValue;
    /** 累计已实现盈亏 */
    private BigDecimal totalProfit;
    /** 今日盈亏（需配合日统计表，可先返回0） */
    private BigDecimal todayProfit;
    /** 持仓列表（简要） */
    private List<PositionBriefVO> positions;
    
    @Data
    @Builder
    public static class PositionBriefVO {
        private String metalType;
        private String metalName;
        private BigDecimal amount;
        private BigDecimal marketValue;
    }
}