package com.gold.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.feign.ProductFeignApi;
import com.gold.common.domain.dto.GoodsPageDTO;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.dto.GoodsAddDTO;
import com.gold.mallApi.domain.dto.GoodsUpdateDTO;
import com.gold.mallApi.domain.dto.SpecAddDTO;
import com.gold.mallApi.domain.dto.SpecUpdateDTO;
import com.gold.mallApi.domain.vo.GoodsAdminDetailVO;
import com.gold.mallApi.domain.vo.GoodsAdminListVO;
import com.gold.mallApi.domain.vo.SpecAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 【修改】管理端商品控制器
 * 原来直接操作 MallGoodsMapper / MallGoodsSpecMapper / GoodsAdminMapper
 * 现
 * 在改为通过 ProductFeignApi 远程调用 gold-product-service
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/goods")
public class GoodsAdminController {

    private final ProductFeignApi productFeignApi;

    /** 查询商品列表（分页） */
    @GetMapping("/list")
    public Result<Page<GoodsAdminListVO>> pageGoods(GoodsPageDTO dto) {
        return productFeignApi.pageGoods(dto);
    }

    /** 查询商品详情（含规格） */
    @GetMapping("/{goodsId}")
    public Result<GoodsAdminDetailVO> getGoodsDetail(@PathVariable Long goodsId) {
        return productFeignApi.getGoodsDetail(goodsId);
    }

    /** 查询单个商品详情，用于回显 */
    @GetMapping("/spec/{specId}")
    public Result<SpecAdminVO> getSpecById(@PathVariable Long specId) {
        return productFeignApi.getSpecById(specId);
    }

    /** 添加商品（含规格） */
    @PostMapping
    public Result<Long> addGoods(@RequestBody GoodsAddDTO dto) {
        return productFeignApi.addGoods(dto);
    }

    /** 修改商品主信息（含规格） */
    @PutMapping("/{goodsId}")
    public Result<Void> updateGoods(@PathVariable Long goodsId, @RequestBody GoodsUpdateDTO dto) {
        return productFeignApi.updateGoods(goodsId, dto);
    }

    /** 删除商品（级联删规格） */
    @DeleteMapping("/{goodsId}")
    public Result<Void> deleteGoods(@PathVariable Long goodsId) {
        return productFeignApi.deleteGoods(goodsId);
    }

    /** 添加商品规格 */
    @PostMapping("/{goodsId}/spec")
    public Result<Long> addSpec(@PathVariable Long goodsId, @RequestBody SpecAddDTO dto) {
        return productFeignApi.addSpec(goodsId, dto);
    }

    /** 修改商品规格 */
    @PutMapping("/spec/{specId}")
    public Result<Void> updateSpec(@PathVariable Long specId, @RequestBody SpecUpdateDTO dto) {
        return productFeignApi.updateSpec(specId, dto);
    }

    /** 删除商品规格 */
    @DeleteMapping("/spec/{specId}")
    public Result<Void> deleteSpec(@PathVariable Long specId) {
        return productFeignApi.deleteSpec(specId);
    }

    /** 更新商品规格库存 */
    @PutMapping("/spec/{specId}/stock")
    public Result<Void> updateStock(@PathVariable Long specId, @RequestParam Integer stock) {
        return productFeignApi.updateStock(specId, stock);
    }

    /** 更新商品规格销售状态 */
    @PutMapping("/spec/{specId}/status")
    public Result<Void> updateSaleStatus(@PathVariable Long specId, @RequestParam Integer saleStatus) {
        return productFeignApi.updateSaleStatus(specId, saleStatus);
    }
}