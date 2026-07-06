package com.gold.order.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项VO
 */
@Data
@Builder
public class OrderListVO {
    private String orderNo;           // 订单号
    private Integer status;           // 0待支付 1已支付 2待核销 3已核销 4已完成 5已取消
    private String statusName;        // 状态中文
    private BigDecimal payAmount;     // 支付金额
    private String goodsName;         // 商品名称（取第一个item）
    private String goodsImage;        // 商品图片
    private Integer quantity;         // 购买数量
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime payTime;    // 支付时间（如有）
}