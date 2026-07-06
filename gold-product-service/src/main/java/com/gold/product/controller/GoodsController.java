package com.gold.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.domain.dto.GoodsPageDTO;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.dto.GoodsAddDTO;
import com.gold.mallApi.domain.dto.GoodsUpdateDTO;
import com.gold.mallApi.domain.dto.SpecAddDTO;
import com.gold.mallApi.domain.dto.SpecUpdateDTO;
import com.gold.mallApi.domain.vo.GoodsAdminDetailVO;
import com.gold.mallApi.domain.vo.GoodsAdminListVO;
import com.gold.mallApi.domain.vo.SpecAdminVO;
import com.gold.product.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/admin/goods")
public class GoodsController {

    private final GoodsService goodsService;

    @GetMapping("/list")
    public Result<Page<GoodsAdminListVO>> pageGoods(GoodsPageDTO dto) {
        return Result.success(goodsService.pageGoods(dto));
    }

    @GetMapping("/{goodsId}")
    public Result<GoodsAdminDetailVO> getGoodsDetail(@PathVariable Long goodsId) {
        return Result.success(goodsService.getGoodsDetail(goodsId));
    }

    @GetMapping("/spec/{specId}")
    public Result<SpecAdminVO> getSpecById(@PathVariable Long specId) {
        return Result.success(goodsService.getSpecById(specId));
    }

    @PostMapping
    public Result<Long> addGoods(@RequestBody GoodsAddDTO dto) {
        return Result.success(goodsService.addGoods(dto));
    }

    @PutMapping("/{goodsId}")
    public Result<Void> updateGoods(@PathVariable Long goodsId, @RequestBody GoodsUpdateDTO dto) {
        dto.setId(goodsId);
        goodsService.updateGoods(dto);
        return Result.success();
    }

    @DeleteMapping("/{goodsId}")
    public Result<Void> deleteGoods(@PathVariable Long goodsId) {
        goodsService.deleteGoods(goodsId);
        return Result.success();
    }

    @PostMapping("/{goodsId}/spec")
    public Result<Long> addSpec(@PathVariable Long goodsId, @RequestBody SpecAddDTO dto) {
        return Result.success(goodsService.addSpec(goodsId, dto));
    }

    @PutMapping("/spec/{specId}")
    public Result<Void> updateSpec(@PathVariable Long specId, @RequestBody SpecUpdateDTO dto) {
        dto.setId(specId);
        goodsService.updateSpec(dto);
        return Result.success();
    }

    @DeleteMapping("/spec/{specId}")
    public Result<Void> deleteSpec(@PathVariable Long specId) {
        goodsService.deleteSpec(specId);
        return Result.success();
    }

    @PutMapping("/spec/{specId}/stock")
    public Result<Void> updateStock(@PathVariable Long specId, @RequestParam Integer stock) {
        goodsService.updateStock(specId, stock);
        return Result.success();
    }

    @PutMapping("/spec/{specId}/status")
    public Result<Void> updateSaleStatus(@PathVariable Long specId, @RequestParam Integer saleStatus) {
        goodsService.updateSaleStatus(specId, saleStatus);
        return Result.success();
    }
}