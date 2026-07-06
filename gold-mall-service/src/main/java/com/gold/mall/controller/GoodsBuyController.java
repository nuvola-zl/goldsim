package com.gold.mall.controller;

import com.gold.api.feign.ProductFeignApi;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.vo.BuyInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 【修改】购买信息控制器
 * 原来直接操作 MallGoodsMapper / MallGoodsSpecMapper / ActivityGoodsMapper / MarketFeignApi
 * 现在改为通过 ProductFeignApi 远程调用 gold-product-service
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mall")
public class GoodsBuyController {

    private final ProductFeignApi productFeignApi;

    @GetMapping("/goods/buy-info/{specId}")
    public Result<BuyInfoVO> getBuyInfo(@PathVariable Long specId,
                                        @RequestParam(defaultValue = "1") Integer quantity) {
        return productFeignApi.getBuyInfo(specId, quantity);
    }
}