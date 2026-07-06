package com.gold.api.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpecInfoDTO {
    /** 商品主ID */
    private Long goodsId;
    /** 商品名称 */
    private String goodsName;
    /** 商品分类 */
    private String category;
    /** 金属材质 */
    private String metalType;
    /** 所属门店ID */
    private Long storeId;

    /** 规格ID */
    private Long specId;
    /** 克重 */
    private BigDecimal weight;
    /** 工费 元/克 */
    private BigDecimal craftFee;

    /** 展示参考金价（管理端录入时的缓存，仅供参考，下单勿直接用） */
    private BigDecimal referencePricePerGram;
    /** 展示参考总价（管理端录入时的缓存，仅供参考，下单勿直接用） */
    private BigDecimal referenceTotalPrice;

    /** 实时金价（下单时从Redis拉取的最新金价） */
    private BigDecimal realTimePricePerGram;
    /** 实时总价（实时金价×克重 + 工费×克重） */
    private BigDecimal realTimeTotalPrice;

    /** 当前库存 */
    private Integer stock;
    /** 冻结库存 */
    private Integer frozenStock;
    /** 可售库存（stock - frozenStock） */
    private Integer availableStock;
    /** 规格封面图 */
    private String coverImage;
    /** 售卖状态 0停售 1在售 */
    private Integer saleStatus;

    // ==================== 活动信息（有进行中活动时返回，null表示无活动） ====================
    /** 活动ID */
    private Long activityId;
    /** 活动名称 */
    private String activityName;
    /** 活动价（有活动时订单服务优先使用这个） */
    private BigDecimal activityPrice;
    /** 活动原价（划线价） */
    private BigDecimal originalPrice;
    /** 活动剩余库存 */
    private Integer activityStock;
    /** 单用户限购数量 */
    private Integer limitPerUser;
}