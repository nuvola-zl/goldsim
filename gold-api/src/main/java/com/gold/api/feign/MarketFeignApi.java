package com.gold.api.feign;

import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.fallback.MarketFeignApiFallback;
import com.gold.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gold-market-service",
        contextId = "marketFeignApi",
        path = "/market/feign",
        fallbackFactory = MarketFeignApiFallback.class
)
public interface MarketFeignApi {

    /**
     * 获取最新价格（模拟盘服务调用）
     */
    @GetMapping("/price/latest")
    Result<MarketPriceDTO> getLatestPrice(@RequestParam("metalType") String metalType);
}