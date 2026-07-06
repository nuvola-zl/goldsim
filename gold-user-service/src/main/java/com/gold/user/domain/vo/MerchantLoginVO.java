package com.gold.user.domain.vo;

import lombok.Data;

@Data
public class MerchantLoginVO {
    private String token;
    private Long merchantId;
    private String merchantName;
}