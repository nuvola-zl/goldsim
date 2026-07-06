package com.gold.simulation.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.simulation.domain.pojo.VirtualTradeRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VirtualTradeRecordMapper extends BaseMapper<VirtualTradeRecord> {

    @Select("SELECT * FROM virtual_trade_record WHERE request_id = #{requestId} LIMIT 1")
    VirtualTradeRecord selectByRequestId(@Param("requestId") String requestId);
}