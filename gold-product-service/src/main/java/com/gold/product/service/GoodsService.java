package com.gold.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.domain.dto.GoodsPageDTO;
import com.gold.common.result.PageResult;
import com.gold.mallApi.domain.dto.GoodsAddDTO;
import com.gold.mallApi.domain.dto.GoodsUpdateDTO;
import com.gold.mallApi.domain.dto.SpecAddDTO;
import com.gold.mallApi.domain.dto.SpecUpdateDTO;
import com.gold.mallApi.domain.vo.*;

import java.util.List;

public interface GoodsService {

    /**
     * 分页查询商品列表（管理端）
     */
    Page<GoodsAdminListVO> pageGoods(GoodsPageDTO dto);

    /**
     * 查询商品详情（管理端，含规格列表）
     */
    GoodsAdminDetailVO getGoodsDetail(Long goodsId);

    /**
     * 添加商品（含规格）
     */
    Long addGoods(GoodsAddDTO dto);

    /**
     * 修改商品主信息
     */
    void updateGoods(GoodsUpdateDTO dto);

    /**
     * 删除商品（级联删除规格）
     */
    void deleteGoods(Long goodsId);

    /**
     * 添加规格
     */
    Long addSpec(Long goodsId, SpecAddDTO dto);

    /**
     * 修改规格
     */
    void updateSpec(SpecUpdateDTO dto);

    /**
     * 删除规格
     */
    void deleteSpec(Long specId);

    /**
     * 修改库存（管理端直接设置）
     */
    void updateStock(Long specId, Integer stock);

    /**
     * 修改售卖状态
     */
    void updateSaleStatus(Long specId, Integer saleStatus);

    /**
     * 查询单个规格详情（用于修改回显端）
     */
    SpecAdminVO getSpecById(Long specId);

    /**
     * 商品搜索
     */
    PageResult<GoodsListVO> searchGoods(String keyword, String category, String metalType, Integer page, Integer size);

    /**
     * 获取商品分类列表
     */
    List<String> getCategories();

    /**
     * 首页聚合数据
     */
    HomePageVO getHomePage();

    // ==================== Mall ====================
    Page<GoodsListVO> getGoodsList(Integer page, Integer size, String category, String metalType);
    GoodsDetailVO getMallGoodsDetail(Long goodsId);
    BuyInfoVO getBuyInfo(Long specId, Integer quantity);
}