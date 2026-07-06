package com.gold.simulation.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class LeaderboardUpdateMessage implements Serializable {
    private Long userId;
    private BigDecimal totalProfit;
}