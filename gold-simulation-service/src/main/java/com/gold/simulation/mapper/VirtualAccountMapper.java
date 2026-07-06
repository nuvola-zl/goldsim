package com.gold.simulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.simulation.domain.pojo.VirtualAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VirtualAccountMapper extends BaseMapper<VirtualAccount> {

    @Select("SELECT * FROM virtual_account WHERE user_id = #{userId}")
    VirtualAccount selectByUserId(@Param("userId") Long userId);
}