package com.gold.api.fallback;

import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.feign.MarketFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
// TODO [命名规范] 类名为 MarketFeignApiFallback 但实现的是 FallbackFactory 接口，应按项目约定命名为 MarketFeignApiFallbackFactory。@FeignClient 中使用的 fallbackFactory 属性功能正确但命名具有误导性
public class MarketFeignApiFallback implements FallbackFactory<MarketFeignApi> {

    @Override
    public MarketFeignApi create(Throwable cause) {
        // 打印远程调用失败原因，方便排查
        log.error("调用行情服务gold-market-service远程接口失败，触发熔断降级，异常信息：{}", cause != null ? cause.getMessage() : "服务不可用");

        // 返回接口代理实现，所有方法走兜底逻辑
        return new MarketFeignApi() {
            // TODO [代码规范] 匿名类中所有方法缺少 @Override 注解。项目中 ProductFeignApiFallbackFactory 的匿名类 21 个方法都加了 @Override，此处应一致
            @Override
            // TODO [降级风险] 行情服务不可用时返回 Result.success() 含硬编码假价格（AU=725, AG=8.4, PT=320），调用方无法区分真实数据和降级数据，可能导致财务计算错误。应改为返回 Result.error() 与项目其他 5 个降级类保持一致，由调用方决定如何处理
            public Result<MarketPriceDTO> getLatestPrice(String metalType) {
                // 【已修复】降级风险
                // 原问题(todo): 返回 Result.success() 含硬编码假价格（AU=725, AG=8.4, PT=320）
                //               调用方无法区分真实数据和降级数据，可能导致财务计算错误
                // 修复(2024-07-10): 返回 Result.error()，与项目其他降级类保持一致
                //                   调用方感知降级，自行决定如何处理
                log.warn("行情服务降级，metalType={}", metalType);
                return Result.error(ErrorCode.MARKET_PRICE_FETCH_FAILED, "行情服务暂不可用，请稍后重试");
            }
        };
    }
}