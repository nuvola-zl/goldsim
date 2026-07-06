package com.gold.api.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品服务Feign调用专用-精简门店DTO
 * 只保留商品详情页需要展示的门店基础信息，轻量化传输
 */
@Data
public class StoreSimpleDTO {
    /** 门店ID */
    private Long storeId;
    /** 门店名称 */
    private String storeName;
    /** 拼接完整地址：省+市+区+详细地址 */
    private String address;
    /** 门店联系电话 */
    private String phone;
    /** 门店营业时间 */
    private String businessHours;
    /** 门店综合评分 */
    private BigDecimal rating;
}