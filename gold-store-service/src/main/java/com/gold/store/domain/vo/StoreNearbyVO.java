package com.gold.store.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 附近门店返回VO
 * 携带门店基础信息 + 当前用户到门店的距离（公里）
 */
@Data
public class StoreNearbyVO {
    /** 门店ID */
    private Long storeId;
    /** 门店名称 */
    private String storeName;
    /** 完整拼接地址：省+市+区+详细地址 */
    private String address;
    /** 门店联系电话 */
    private String phone;
    /** 门店营业时间 */
    private String businessHours;
    /** 门店封面图URL */
    private String coverImage;
    /** 门店综合评分 0.0~5.0 */
    private BigDecimal rating;
    /** 用户到门店距离，单位公里，保留两位小数 */
    private Double distance;
}