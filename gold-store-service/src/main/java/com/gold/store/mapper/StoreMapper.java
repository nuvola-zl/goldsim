package com.gold.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gold.store.domain.po.Store;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoreMapper extends BaseMapper<Store> {
}