package com.gold.api.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketPriceDTO {
    /** 品种编码 AU黄金 AG白银 PT铂金 */
    private String metalType;
    /** 品种名称 */
    private String metalName;
    /** 开盘价（美元/盎司） */
    private BigDecimal openPrice;
    /** 最高价（美元/盎司） */
    private BigDecimal highPrice;
    /** 最低价（美元/盎司） */
    private BigDecimal lowPrice;
    /** 昨日收盘价（美元/盎司） */
    private BigDecimal preClosePrice;
    /** 最新实时价格（美元/盎司） */
    private BigDecimal latestPrice;
    /** 涨跌幅，例：+2.15% / -0.80% */
    private String changePercent;
    /** 更新时间 */
    private LocalTime updateTime;

    /** true=模拟数据，false/null=真实数据 */
    private boolean mockData;
}