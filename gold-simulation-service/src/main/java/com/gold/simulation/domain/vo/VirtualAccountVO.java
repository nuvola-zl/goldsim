package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 虚拟账户信息VO
 * 接口：GET /api/simulation/account/info
 */
@Data
@Builder
public class VirtualAccountVO {
    /** 用户ID */
    private Long userId;
    /** 可用虚拟余额 */
    private BigDecimal balance;
    /** 累计已实现总盈亏 */
    private BigDecimal totalProfit;
    /** 累计总手续费 */
    private BigDecimal totalFee;
    /** 交易次数 */
    private Integer tradeCount;
}