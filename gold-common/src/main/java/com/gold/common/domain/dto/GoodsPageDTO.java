package com.gold.common.domain.dto;

import lombok.Data;

@Data
public class GoodsPageDTO {
    private String goodsName;
    private String category;
    private String metalType;
    private Integer page = 1;
    private Integer size = 10;
}