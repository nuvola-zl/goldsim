package com.gold.api.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class PayNoticeMessage implements Serializable {
    // 下单用户ID
    private Long userId;
    // 订单编号
    private String orderNo;
    // 实付金额
    private BigDecimal payAmount;
    // 商品名称
    private String goodsName;
    // 支付超时分钟
    private Integer expireMinute;
}