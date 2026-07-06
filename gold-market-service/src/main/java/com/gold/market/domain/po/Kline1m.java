package com.gold.market.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1分钟标准K线表
 * 存储每分钟行情，提供前端蜡烛图绘制数据源
 * open/close为真实接口价格；high/low本地模拟仅用于可视化
 */
@Data
@TableName("kline_1m")
public class Kline1m {

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
     * K线所属完整分钟时间戳（对齐0秒0微秒，例2026-06-21 14:00:00）
     */
    private LocalDateTime timestamp;

    /**
     * 该分钟开盘价，本次拉取真实接口价格
     */
    private BigDecimal openPrice;

    /**
     * 该分钟区间最高价，本地随机模拟，仅绘图展示，不参与交易计算
     */
    private BigDecimal highPrice;

    /**
     * 该分钟区间最低价，本地随机模拟，仅绘图展示，不参与交易计算
     */
    private BigDecimal lowPrice;

    /**
     * 该分钟收盘价，下一分钟同步行情后填充真实价格，未走完时为null
     */
    private BigDecimal closePrice;

    /**
     * 成交量：统计该分钟内用户成交订单笔数，前端成交量柱状图使用
     */
    private Long volume;

    /**
     * K线记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * K线最后更新时间（模拟高低、填充收盘时自动更新）
     */
    private LocalDateTime updateTime;
}