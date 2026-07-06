package com.gold.product.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.mallApi.domain.vo.GoodsListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsShowMapper {


    /**
     * 第一步：分页查商品基础信息
     */
    Page<GoodsListVO> selectGoodsBaseList(Page<GoodsListVO> page,
                                          @Param("category") String category,
                                          @Param("metalType") String metalType);

    /**
     * 第二步：批量查默认规格（IN列表只有一页数据量，10~20个，毫无压力）
     */
    List<GoodsListVO> selectDefaultSpecsByGoodsIds(@Param("goodsIds") List<Long> goodsIds);
}