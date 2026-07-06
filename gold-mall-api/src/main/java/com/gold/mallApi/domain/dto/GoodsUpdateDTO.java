package com.gold.mallApi.domain.dto;

import lombok.Data;

/**
 * 修改商品主信息请求
 */
@Data
public class GoodsUpdateDTO {
    private Long id;
    private String goodsName;
    private String category;
    private String metalType;
    private Long storeId;
    private Integer sort;
}