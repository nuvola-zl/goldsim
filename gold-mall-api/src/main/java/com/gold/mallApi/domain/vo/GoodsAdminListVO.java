package com.gold.mallApi.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端商品列表VO
 */
@Data
public class GoodsAdminListVO {
    private Long goodsId;
    private String goodsName;
    private String category;
    private String metalType;
    private Long storeId;
    private Integer sort;
    private Integer specCount;      // 规格数量
    private Integer totalStock;     // 总库存（所有规格库存之和）
    private LocalDateTime createTime;
}