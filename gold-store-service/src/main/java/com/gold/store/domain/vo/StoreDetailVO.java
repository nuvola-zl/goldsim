package com.gold.store.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 门店详情VO
 * 用户点击门店进入详情页返回完整信息
 * 包含经纬度用于前端地图导航、拆分省市区便于页面分层展示
 */
@Data
public class StoreDetailVO {

    /**
     * 门店主键ID
     */
    private Long storeId;

    /**
     * 门店名称
     */
    private String storeName;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区/县
     */
    private String district;

    /**
     * 详细街道地址
     */
    private String address;

    /**
     * 经度，前端地图导航使用
     */
    private BigDecimal longitude;

    /**
     * 纬度，前端地图导航使用
     */
    private BigDecimal latitude;

    /**
     * 门店联系电话
     */
    private String phone;

    /**
     * 门店营业时间
     */
    private String businessHours;

    /**
     * 门店封面图URL
     */
    private String coverImage;

    /**
     * 门店综合评分 0.0 ~ 5.0
     */
    private BigDecimal rating;

    /**
     * 门店评价总人数
     */
    private Integer ratingCount;
}