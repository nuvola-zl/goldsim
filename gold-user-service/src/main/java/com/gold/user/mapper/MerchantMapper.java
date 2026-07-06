package com.gold.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.gold.user.domain.pojo.Merchant;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MerchantMapper extends BaseMapper<Merchant> {

    @Select("SELECT * FROM merchant WHERE username = #{username}")
    Merchant selectByUsername(@Param("username") String username);
}