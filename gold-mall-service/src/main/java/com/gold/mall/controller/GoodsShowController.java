package com.gold.mall.controller;

import com.gold.api.feign.ProductFeignApi;
import com.gold.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.mallApi.domain.vo.GoodsDetailVO;
import com.gold.mallApi.domain.vo.GoodsListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 【修改】商城商品展示控制器
 * 原来直接操作 MallGoodsMapper / MallGoodsSpecMapper / GoodsShowMapper / ActivityGoodsMapper
 * 现在改为通过 ProductFeignApi 远程调用 gold-product-service
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mall")
public class GoodsShowController {

    private final ProductFeignApi productFeignApi;

    @GetMapping("/goods/list")
    public Result<Page<GoodsListVO>> getGoodsList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String metalType
) {
        return productFeignApi.getGoodsList(page, size, category, metalType);
    }

    @GetMapping("/goods/{id}")
    public Result<GoodsDetailVO> getGoodsDetail(@PathVariable Long id) {
        return productFeignApi.getMallGoodsDetail(id);
    }
}