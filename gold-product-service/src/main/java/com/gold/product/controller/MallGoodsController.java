package com.gold.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.vo.BuyInfoVO;
import com.gold.mallApi.domain.vo.GoodsDetailVO;
import com.gold.mallApi.domain.vo.GoodsListVO;
import com.gold.mallApi.domain.vo.HomePageVO;
import com.gold.product.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/mall")
public class MallGoodsController {

    private final GoodsService goodsService;

    @GetMapping("/goods/list")
    public Result<Page<GoodsListVO>> getGoodsList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String metalType) {
        return Result.success(goodsService.getGoodsList(page, size, category, metalType));
    }

    @GetMapping("/goods/{id}")
    public Result<GoodsDetailVO> getGoodsDetail(@PathVariable Long id) {
        return Result.success(goodsService.getMallGoodsDetail(id));
    }

    @GetMapping("/goods/buy-info/{specId}")
    public Result<BuyInfoVO> getBuyInfo(@PathVariable Long specId,
                                        @RequestParam(defaultValue = "1") Integer quantity) {
        return Result.success(goodsService.getBuyInfo(specId, quantity));
    }

    /**
     * 商品搜索
     * 接口：GET /api/mall/search?keyword=&category=&metalType=&page=&size=
     */
    @GetMapping("/search")
    public Result<PageResult<GoodsListVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String metalType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        return Result.success(goodsService.searchGoods(keyword, category, metalType, page, size));
    }

    /**
     * 商品分类列表
     * 接口：GET /api/mall/category/list
     */
    @GetMapping("/category/list")
    public Result<List<String>> getCategories() {
        return Result.success(goodsService.getCategories());
    }

    /**
     * 首页聚合数据
     * 接口：GET /api/mall/home
     */
    @GetMapping("/home")
    public Result<HomePageVO> getHomePage() {
        return Result.success(goodsService.getHomePage());
    }
}