package com.gold.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.market.domain.po.Kline1m;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


import java.time.LocalDateTime;
import java.util.List;

public interface Kline1mMapper extends BaseMapper<Kline1m> {

    @Select("SELECT * FROM kline_1m WHERE metal_type = #{metalType} AND timestamp = #{timestamp} LIMIT 1")
    Kline1m selectByMetalAndTime(@Param("metalType") String metalType,
                                 @Param("timestamp") LocalDateTime timestamp);

    @Select("SELECT * FROM kline_1m WHERE metal_type = #{metalType} ORDER BY timestamp DESC LIMIT #{limit}")
    List<Kline1m> selectRecent(@Param("metalType") String metalType,
                               @Param("limit") int limit);

    @Select("SELECT * FROM kline_1m WHERE metal_type = #{metalType} AND timestamp >= #{today} AND timestamp < #{tomorrow} ORDER BY timestamp ASC")
    List<Kline1m> selectTodayKline(@Param("metalType") String metalType,
                                   @Param("today") LocalDateTime today,
                                   @Param("tomorrow") LocalDateTime tomorrow);
}
