package com.gold.mallApi.domain.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 修改规格请求
 */
@Data
public class SpecUpdateDTO {
    private Long id;
    private BigDecimal weight;
    private BigDecimal craftFee;
    private Integer stock;
    private String coverImage;
    private List<String> detailImages;
    private String description;
    private Integer saleStatus;
}