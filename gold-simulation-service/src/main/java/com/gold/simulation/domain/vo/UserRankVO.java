package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserRankVO {
    private Long rank;             // 个人名次（未上榜为null）
    private BigDecimal totalProfit; // 累计盈亏（未上榜为null）
    private Boolean onList;        // 是否在前20名
}