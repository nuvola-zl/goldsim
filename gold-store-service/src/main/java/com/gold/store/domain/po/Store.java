package com.gold.store.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 线下实体门店信息表 实体
 * 对应数据库表：store
 * 业务场景：到店自提、附近门店查询、门店核销、门店评分展示
 */
@Data
@TableName("store")
public class Store {

    /**
     * 门店主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 门店名称，示例：长沙五一广场黄金旗舰店
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
     * 详细收货/提货地址
     */
    private String address;

    /**
     * 经度，Redis Geo地理位置计算核心字段
     * 精度：DECIMAL(10,7)，适配高德/百度地图坐标
     */
    private BigDecimal longitude;

    /**
     * 纬度，Redis Geo地理位置计算核心字段
     */
    private BigDecimal latitude;

    /**
     * 门店联系电话
     */
    private String phone;

    /**
     * 门店营业时间，格式示例：09:00-21:00
     */
    private String businessHours;

    /**
     * 门店封面图URL，用于门店列表、首页推荐展示
     */
    private String coverImage;

    /**
     * 门店综合评分，范围 0.0 ~ 5.0
     * 初始默认5.0，用户核销完成后可评价更新
     */
    private BigDecimal rating;

    /**
     * 门店总评价人数，每新增一条评价+1
     */
    private Integer ratingCount;

    /**
     * 门店营业状态
     * 0 = 停业（不可下单、不可核销）
     * 1 = 营业中（正常可下单、到店提货核销）
     */
    private Integer status;

    /**
     * 记录创建时间，自动填充
     */
    private LocalDateTime createTime;

    /**
     * 记录更新时间，修改时自动更新
     */
    private LocalDateTime updateTime;
}