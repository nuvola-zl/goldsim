package com.gold.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mall_activity_goods")
public class MallActivityGoods {
    /**
     * 活动商品关联主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联活动id，绑定MallActivity.id
     */
    private Long activityId;

    /**
     * 关联商品规格id，绑定MallGoodsSpec.id
     */
    private Long specId;

    /**
     * 活动原价，活动未开启时商品正常售卖总价
     */
    private BigDecimal originalPrice;

    /**
     * 活动抢购价，活动期间用户下单使用的优惠总价
     */
    private BigDecimal activityPrice;

    /**
     * 活动专属库存，独立于商品日常库存，抢购仅消耗该库存
     */
    private Integer activityStock;

    /**
     * 单用户本场活动限购数量，默认1件
     */
    private Integer limitPerUser;

    /**
     * 本场活动该规格已售出数量
     */
    private Integer soldCount;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;
}