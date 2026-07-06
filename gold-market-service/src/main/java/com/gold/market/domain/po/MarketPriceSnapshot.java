package com.gold.market.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 实时价格快照表
 * 每个品种仅保留最新一条行情，用于首页展示实时价格、当日全天行情指标
 */
@Data
@TableName("market_price_snapshot")
public class MarketPriceSnapshot {

    /**
     * 自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 品种类型：AU黄金 / AG白银 / PT铂金
     */
    private String metalType;

    /**
     * 品种全称，如Au99.99、Ag(T+D)
     */
    private String metalName;

    /**
     * 当前最新成交价（用户买卖成交以此价格为准，真实接口数据）
     */
    private BigDecimal latestPrice;

    /**
     * 交易所当日全天开盘价（接口返回全天指标，非单分钟区间）
     */
    private BigDecimal openPrice;

    /**
     * 交易所当日全天最高价（接口返回全天极值）
     */
    private BigDecimal highPrice;

    /**
     * 交易所当日全天最低价（接口返回全天极值）
     */
    private BigDecimal lowPrice;

    /**
     * 上一交易日收盘价格
     */
    private BigDecimal preClosePrice;

    /**
     * 涨跌幅文本，例：+0.39%、-1.20%
     */
    private String changePercent;

    /**
     * 接口原始行情时间，仅时分秒，如14:25:30
     */
    private LocalTime quoteTime;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录最后修改时间，更新快照自动刷新
     */
    private LocalDateTime modifyTime;
}