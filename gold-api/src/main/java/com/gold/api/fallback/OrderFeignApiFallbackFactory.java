package com.gold.api.fallback;

import com.gold.api.feign.OrderFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderFeignApiFallbackFactory implements FallbackFactory<OrderFeignApi> {

    @Override
    public OrderFeignApi create(Throwable cause) {
        log.error("调用订单服务gold-order-service远程接口失败，触发熔断降级，异常信息：{}",
                cause != null ? cause.getMessage() : "服务不可用");

        return new OrderFeignApi() {
            @Override
            public Result<String> generatePayHtml(String orderNo, Long userId) {
                log.warn("订单服务降级，orderNo={}", orderNo);
                return Result.error(ErrorCode.SYSTEM_BUSY, "订单服务暂不可用，请稍后重试");
            }
        };
    }
}