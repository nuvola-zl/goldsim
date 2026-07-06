package com.gold.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.domain.dto.GoodsPageDTO;
import com.gold.mallApi.domain.vo.GoodsAdminListVO;
import com.gold.mallApi.domain.vo.SpecAdminVO;
import com.gold.product.entity.MallGoodsSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GoodsAdminMapper extends BaseMapper<MallGoodsSpec> {

    /**
     * 管理端分页查询商品列表（带规格数量和总库存统计）
     */
    Page<GoodsAdminListVO> selectGoodsAdminPage(Page<GoodsAdminListVO> page,
                                                @Param("dto") GoodsPageDTO dto);

    /**
     * 查询商品下所有规格（管理端用）
     */
    @Select("SELECT id as specId, weight, craft_fee, price_per_gram, total_price, " +
            "stock, frozen_stock, sale_status, cover_image, detail_images, description, " +
            "create_time, update_time " +
            "FROM mall_goods_spec WHERE goods_id = #{goodsId} ORDER BY weight ASC")
    List<SpecAdminVO> selectSpecsByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 原子扣减库存（where stock >= num）
     */
    @Update("UPDATE mall_goods_spec SET stock = stock - #{num} WHERE id = #{specId} AND stock >= #{num}")
    int updateStockReduce(@Param("specId") Long specId, @Param("num") Integer num);

    /**
     * 直接修改库存（管理端用，不走原子扣减逻辑）
     */
    @Update("UPDATE mall_goods_spec SET stock = #{stock} WHERE id = #{specId}")
    int updateStockDirect(@Param("specId") Long specId, @Param("stock") Integer stock);

    /**
     * 修改售卖状态
     */
    @Update("UPDATE mall_goods_spec SET sale_status = #{saleStatus} WHERE id = #{specId}")
    int updateSaleStatus(@Param("specId") Long specId, @Param("saleStatus") Integer saleStatus);

    /**
     * 统计商品下规格数量
     */
    @Select("SELECT COUNT(*) FROM mall_goods_spec WHERE goods_id = #{goodsId}")
    int countSpecByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 统计商品下总库存
     */
    @Select("SELECT COALESCE(SUM(stock), 0) FROM mall_goods_spec WHERE goods_id = #{goodsId}")
    int sumStockByGoodsId(@Param("goodsId") Long goodsId);
}