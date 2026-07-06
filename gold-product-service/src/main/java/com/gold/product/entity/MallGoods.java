package com.gold.product.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mall_goods")
public class MallGoods {
    /**
     * 商品主表自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品通用名称，同一款商品多规格共用名称，不分克重
     */
    private String goodsName;

    /**
     * 商品分类标识
     * CATEGORY_GOLD_BAR：投资金条
     * CATEGORY_JEWELRY：首饰
     * CATEGORY_COIN：金币
     */
    private String category;

    /**
     * 金属材质标识
     * AU：黄金
     * AG：白银
     * PT：铂金
     */
    private String metalType;

    /**
     * 所属店铺ID，预留多门店架构，后续对接门店表
     */
    private Long storeId;

    /**
     * 前台商品排序权重，数值越小展示越靠前
     */
    private Integer sort;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录更新时间，数据修改自动刷新
     */
    private LocalDateTime updateTime;
}