package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模拟盘交易规则配置VO
 */
@Data
@Builder
public class SimulationConfigVO {
    private BigDecimal initialBalance;      // 初始体验金
    private BigDecimal feeRate;             // 手续费率
    private BigDecimal minTradeAmount;      // 最小交易单位
    private List<String> supportedMetals;     // 支持的金属类型
    private List<MetalConfig> metalList;    // 各金属详细配置

    @Data
    @Builder
    public static class MetalConfig {
        private String metalType;     // AU / AG / PT
        private String metalName;     // 黄金/白银/铂金
        private String unit;          // 元/克
    }
}