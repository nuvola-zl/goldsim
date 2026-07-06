package com.gold.api.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 门店对外传输DTO（Feign远程调用专用）
 * 存放于公共api模块，供订单服务、其他微服务远程调用门店服务时接收数据
 * 核心用途：订单查询详情时展示提货门店信息、下单校验门店基础信息
 */
@Data
public class StoreInfoDTO {

    /**
     * 门店主键ID
     */
    private Long storeId;

    /**
     * 门店名称，例：长沙五一广场黄金旗舰店
     */
    private String storeName;

    /**
     * 省份名称
     */
    private String province;

    /**
     * 城市名称
     */
    private String city;

    /**
     * 区县名称
     */
    private String district;

    /**
     * 门店详细街道地址
     */
    private String address;

    /**
     * 门店联系电话，用户提货时可联系门店
     */
    private String phone;

    /**
     * 门店营业时间，格式示例：09:00-21:00
     */
    private String businessHours;

    /**
     * 门店经度坐标，前端订单详情页导航使用
     */
    private BigDecimal longitude;

    /**
     * 门店纬度坐标，前端订单详情页导航使用
     */
    private BigDecimal latitude;

    /**
     * 门店封面图片地址
     */
    private String coverImage;

    /**
     * 门店综合评分，范围0.0~5.0
     */
    private BigDecimal rating;
}