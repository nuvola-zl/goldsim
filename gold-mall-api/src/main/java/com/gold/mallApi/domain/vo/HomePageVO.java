package com.gold.mallApi.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 首页聚合数据 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePageVO {
    private List<GoodsListVO> recommendGoods;     // 推荐商品
    private List<ActivityShowVO> hotActivities;   // 热门活动
    private List<MetalPriceVO> latestPrices;        // 最新金价

    /**
     * 金属实时价格子VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetalPriceVO {
        private String metalType;      // AU / AG / PT
        private String metalName;      // 黄金 / 白银 / 铂金
        private BigDecimal latestPrice;  // 最新价
        private String changePercent;    // 涨跌幅
    }
}