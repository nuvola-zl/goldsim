package com.gold.market.controller;

import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.feign.MarketFeignApi;
import com.gold.common.result.Result;
import com.gold.market.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/market/feign")
public class MarketFeignController implements MarketFeignApi {

    private final MarketPriceService marketPriceService;

    @Override
    @GetMapping("/price/latest")
    public Result<MarketPriceDTO> getLatestPrice(@RequestParam("metalType") String metalType) {
        MarketPriceDTO dto = marketPriceService.getLatestPrice(metalType);
        if (dto == null) {
            return Result.error("A00001","暂无行情数据");
        }
        return Result.success(dto);
    }
}