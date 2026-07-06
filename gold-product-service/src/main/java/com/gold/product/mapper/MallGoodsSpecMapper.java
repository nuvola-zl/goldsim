package com.gold.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.product.entity.MallGoodsSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MallGoodsSpecMapper extends BaseMapper<MallGoodsSpec> {

    /**
     * 扣减商品库存
     * @param specId 规格主键
     * @param num 扣减数量
     * @return 影响行数
     */
    int updateStockReduce(Long specId, Integer num);

    /**
     * 下单：冻结库存，frozen_stock += num
     */
    int freezeStock(Long specId, Integer num);

    /**
     * 取消/超时关单：解冻库存，frozen_stock -= num
     */
    int unFreezeStock(Long specId, Integer num);

    /** 【新增】支付成功：仅扣减总库存（配合unFreezeStock两步操作） */
    int deductStockOnly(@Param("specId") Long specId, @Param("num") Integer num);
}