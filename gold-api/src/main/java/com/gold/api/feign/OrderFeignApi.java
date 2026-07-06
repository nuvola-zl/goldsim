package com.gold.api.feign;

import com.gold.api.fallback.OrderFeignApiFallbackFactory;
import com.gold.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "gold-order-service",
        fallbackFactory = OrderFeignApiFallbackFactory.class  // 【新增】
    )
    /**
     * 【已修复】缺失降级 + 参数隐患
     *
     * 原问题(todo):
     * - 唯一没有 fallback/fallbackFactory 的 Feign 客户端，服务不可用时直接抛异常
     * - @PathVariable 缺少显式 value，依赖 -parameters 编译标志
     *
     * 修复内容(2024-07-10):
     * - 添加 fallbackFactory = OrderFeignApiFallbackFactory.class
     * - @PathVariable 增加显式 value="orderNo"
     */
    public interface OrderFeignApi {
    @PostMapping("/order/pay/{orderNo}")
    Result<String> generatePayHtml(
            @PathVariable("orderNo") String orderNo,  // 【新增】显式 value
            @RequestParam("userId") Long userId       // 【新增】显式 value（顺便补上）
    );
}