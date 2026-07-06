package com.gold.product.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.gold.common.config.JsonListTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("mall_goods_spec")
public class MallGoodsSpec {
    /**
     * 规格自增主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联商品主表id，绑定MallGoods.id
     */
    private Long goodsId;

    /**
     * 商品克重，单位克，保留两位小数，例如10.00、50.00
     */
    private BigDecimal weight;

    /**
     * 工费，单位元/克，默认0，首饰、金条加工工艺费
     */
    private BigDecimal craftFee;

    /**
     * 展示用实时基准金价（元/克），仅页面展示缓存，下单实时拉取行情服务最新金价计算
     */
    private BigDecimal pricePerGram;

    /**
     * 页面展示总价，计算公式：实时金价×克重 + 工费×克重，仅前端展示，下单实时重算
     */
    private BigDecimal totalPrice;

    /**
     * 日常实物库存数量，支付成功后扣减库存
     */
    private Integer stock;

    /**
     * 规格售卖状态
     * 0：停售（缺货/停产，前端置灰不可选择）
     * 1：正常在售
     */
    private Integer saleStatus;



    /**
     * 当前规格独立封面图片URL，不同克重规格可配置单独图片
     */
    private String coverImage;

    /**
     * 规格详情轮播图，JSON数组存储多张图片链接
     */
    // ✅ 直接用这个，List<String>、List<YourObject> 都能自动处理
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> detailImages;


    /**
     * 规格专属富文本描述，存放该克重专属工艺、证书、成色说明
     */
    private String description;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录更新时间，数据修改自动刷新
     */
    private LocalDateTime updateTime;

    /**
     * 冻结字段
     */
    private Integer frozenStock;
}