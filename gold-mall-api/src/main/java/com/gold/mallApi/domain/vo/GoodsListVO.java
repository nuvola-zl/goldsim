package com.gold.mallApi.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsListVO {
    private Long goodsId;
    private String goodsName;
    private String category;
    private String metalType;
    private Long storeId;
    
    // 默认展示规格信息
    private Long specId;
    private BigDecimal weight;
    private BigDecimal totalPrice;
    private String coverImage;


    //追加
    private Long activityId;      // 有活动=活动ID，无活动=null
}