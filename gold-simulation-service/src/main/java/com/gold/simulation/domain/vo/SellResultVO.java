package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卖出交易结果返回VO
 * 用户卖出贵金属成交后，返回前端的完整交易信息
 */
@Data
@Builder
public class SellResultVO {

    /**
     * 全局唯一请求ID，幂等防重标识
     * 前端下单生成，可用于查询对应交易流水、排查重复请求
     */
    private String requestId;

    /**
     * 贵金属类型
     * AU=黄金、AG=白银、PT=铂金
     */
    private String metalType;

    /**
     * 交易类型，固定值 SELL 代表卖出
     */
    private String tradeType;

    /**
     * 本次卖出克重，保留4位小数
     */
    private BigDecimal amount;

    /**
     * 成交单价，单位：元/克
     * 取自行情服务实时最新贵金属价格
     */
    private BigDecimal price;

    /**
     * 卖出总成交额 = 卖出克重 × 成交单价
     * 未扣除手续费的原始总金额
     */
    private BigDecimal tradeValue;

    /**
     * 本次卖出手续费，统一费率0.1%
     */
    private BigDecimal fee;

    /**
     * 本次卖出已实现盈亏
     * 计算公式：卖出总金额 - 持仓对应成本 - 本次手续费
     * 正数盈利，负数亏损，会同步累加至用户账户累计盈亏
     */
    private BigDecimal realizedProfit;

    /**
     * 卖出实际入账金额
     * 计算公式：成交总额 - 手续费，直接增加到用户虚拟余额
     */
    private BigDecimal netIncome;

    /**
     * 卖出完成后，用户最新虚拟可用余额
     */
    private BigDecimal balanceAfter;

    /**
     * 成交时间，对应交易流水创建时间
     */
    private LocalDateTime timestamp;

    /**
     * 用户当前持仓总盈亏金额，保留4位小数
     */
    private BigDecimal totalProfit;
}