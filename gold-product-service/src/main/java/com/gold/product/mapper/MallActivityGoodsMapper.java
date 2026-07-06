package com.gold.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.product.entity.MallActivityGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MallActivityGoodsMapper extends BaseMapper<MallActivityGoods> {

    /**
     * 支付成功：增加 sold_count
     */
    int increaseSoldCount(@Param("activityId") Long activityId,
                          @Param("specId") Long specId,
                          @Param("quantity") Integer quantity);

    /**
     * 取消订单：减少 sold_count
     */
    int decreaseSoldCount(@Param("activityId") Long activityId,
                          @Param("specId") Long specId,
                          @Param("quantity") Integer quantity);
}