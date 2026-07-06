package com.gold.simulation.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 买入交易返回VO
 * 用户发起买入黄金/白银/铂金成功后，返回给前端的交易结果详情
 */
@Data
@Builder
public class BuyResultVO {

    /**
     * 全局唯一请求ID，幂等标识
     * 前端下单时生成，用于防止重复提交、排查交易流水
     */
    private String requestId;

    /**
     * 贵金属类型
     * AU-黄金 AG-白银 PT-铂金
     */
    private String metalType;

    /**
     * 交易类型，固定为 BUY
     */
    private String tradeType;

    /**
     * 本次买入克重，保留4位小数
     */
    private BigDecimal amount;

    /**
     * 成交单价，单位：元/克
     * 取自行情服务实时金价
     */
    private BigDecimal price;

    /**
     * 交易原始总额 = 买入克重 × 成交单价
     * 未扣除手续费的本金金额
     */
    private BigDecimal tradeValue;

    /**
     * 本次买入手续费，费率0.1%
     */
    private BigDecimal fee;

    /**
     * 本次实际支出总金额 = 交易总额 + 手续费
     * 从用户虚拟余额中扣除的金额
     */
    private BigDecimal totalCost;

    /**
     * 买入完成后，用户剩余虚拟可用余额
     */
    private BigDecimal balanceAfter;

    /**
     * 成交时间戳，即本次交易创建时间
     */
    private LocalDateTime timestamp;
}