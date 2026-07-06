package com.gold.mallApi.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端商品详情VO
 */
@Data
public class GoodsAdminDetailVO {
    private Long goodsId;
    private String goodsName;
    private String category;
    private String metalType;
    private Long storeId;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<SpecAdminVO> specList;
}