package com.gold.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.product.entity.StockDeductRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StockDeductRecordMapper extends BaseMapper<StockDeductRecord> {
    
    /**
     * 检查这个订单的库存是否已经扣减过
     * 用于MQ幂等：同一个biz_key只能扣一次
     */
    @Select("SELECT COUNT(*) FROM stock_deduct_record WHERE biz_key = #{bizKey}")
    int existsByBizKey(@Param("bizKey") String bizKey);
}