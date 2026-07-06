package com.gold.order.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailVO {
    // 订单信息
    private String orderNo;
    private Integer status;           // 0-待支付 1-已支付
    private BigDecimal payAmount;
    private LocalDateTime createTime;
    
    // 商品信息
    private String goodsName;
    private String goodsImage;
    private BigDecimal weight;
    private Integer quantity;
    private BigDecimal unitPrice;
    
    // 门店信息（调门店服务获取）
    private String storeName;
    private String storeAddress;      // 完整地址
    private String storePhone;
    private String businessHours;
}