package com.gold.simulation.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.simulation.domain.pojo.VirtualPosition;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

public interface VirtualPositionMapper extends BaseMapper<VirtualPosition> {

    @Select("SELECT * FROM virtual_position WHERE user_id = #{userId} AND metal_type = #{metalType}")
    VirtualPosition selectByUserAndMetal(@Param("userId") Long userId,
                                          @Param("metalType") String metalType);
}