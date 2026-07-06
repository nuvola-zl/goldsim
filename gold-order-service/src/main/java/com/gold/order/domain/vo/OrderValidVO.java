package com.gold.order.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付校验结果VO
 */
@Data
@Builder
public class OrderValidVO {
    private Boolean valid;            // 是否可支付
    private String orderNo;           // 订单号
    private BigDecimal payAmount;     // 应支付金额
    private String goodsName;         // 商品名称
    private Integer status;           // 当前状态
    private String failReason;        // 不可支付的原因
    private Long expireSeconds;       // 剩余支付秒数（如15分钟倒计时）
}