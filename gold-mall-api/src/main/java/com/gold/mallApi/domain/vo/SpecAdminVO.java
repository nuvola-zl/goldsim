package com.gold.mallApi.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端规格VO
 */
@Data
public class SpecAdminVO {
    private Long specId;
    private BigDecimal weight;
    private BigDecimal craftFee;
    private BigDecimal pricePerGram;
    private BigDecimal totalPrice;
    private Integer stock;
    private Integer frozenStock;
    private Integer saleStatus;
    private String coverImage;
    private List<String> detailImages;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}