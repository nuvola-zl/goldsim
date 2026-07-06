package com.gold.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.product.entity.StockDeductFailRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface StockDeductFailRecordMapper extends BaseMapper<StockDeductFailRecord> {

    /**
     * 查询待处理且重试次数小于上限的记录
     * 用于定时任务自动补偿
     *
     * @param status 状态（0=待处理）
     * @param maxRetryCount 最大重试次数
     */
    @Select("SELECT * FROM stock_deduct_fail_record " +
            "WHERE status = #{status} AND retry_count < #{maxRetryCount} " +
            "ORDER BY create_time ASC")
    List<StockDeductFailRecord> selectByStatusAndRetryCount(
            @Param("status") Integer status,
            @Param("maxRetryCount") Integer maxRetryCount);
}