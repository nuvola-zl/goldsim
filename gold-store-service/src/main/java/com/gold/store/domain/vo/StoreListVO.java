package com.gold.store.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 门店列表展示VO
 * 用于前端分页查询门店列表、附近门店列表返回
 * 只保留列表页需要展示的精简字段，隐藏经纬度等后台计算字段
 */
@Data
public class StoreListVO {

    /**
     * 门店主键ID
     */
    private Long storeId;

    /**
     * 门店名称（如：长沙五一广场旗舰店）
     */
    private String storeName;

    /**
     * 完整地址：省+市+区+详细地址拼接字符串
     * 前端列表直接展示，无需前端自行拼接
     */
    private String address;

    /**
     * 门店联系电话
     */
    private String phone;

    /**
     * 门店营业时间，示例：09:00-21:00
     */
    private String businessHours;

    /**
     * 门店封面图片URL，列表页展示门店图片
     */
    private String coverImage;

    /**
     * 门店综合评分 0.0 ~ 5.0
     */
    private BigDecimal rating;

    /**
     * 门店总评价人数
     */
    private Integer ratingCount;
}