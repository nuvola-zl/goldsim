package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LeaderboardVO {
    private int rank;              // 名次 1-20
    private Long userId;           // 用户ID
    private String nickname;       // 昵称（无则显示"用户xxx"）
    private BigDecimal totalProfit; // 累计已实现盈亏
}