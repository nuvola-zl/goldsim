package com.gold.api.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeductStockMqMessage implements Serializable {
    
    private String orderNo;
    private Long specId;
    private Integer quantity;
    
    // 用于幂等
    private String bizKey;
}