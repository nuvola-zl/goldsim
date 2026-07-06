package com.gold.api.fallback;

import com.gold.api.domain.dto.StoreInfoDTO;
import com.gold.api.domain.dto.StoreSimpleDTO;
import com.gold.api.feign.StoreFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 门店服务Feign降级工厂
 * 调用gold-store-service超时/熔断/下线时触发降级
 */
@Slf4j
@Component
public class StoreFeignApiFallbackFactory implements FallbackFactory<StoreFeignApi> {

    @Override
    public StoreFeignApi create(Throwable cause) {
        log.error("【门店服务熔断降级】gold-store-service调用异常：{}", cause.getMessage(), cause);
        return new StoreFeignApi() {
            /**
             * 查门店信息（订单服务调用）
             */
            @Override
            public Result<StoreInfoDTO> getStoreInfo(Long storeId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "门店服务暂时不可用，查询门店信息失败");
            }

            /**
             * 校验门店是否营业（订单服务调用）
             */
            @Override
            public Result<Boolean> isStoreValid(Long storeId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "门店服务暂时不可用，无法校验门店营业状态");
            }

            // 新增：商品服务专用，获取精简门店信息
            @Override
            public Result<StoreSimpleDTO> getStoreSimple(Long storeId) {
                return Result.error(ErrorCode.SYSTEM_BUSY, "门店服务暂时不可用，获取门店精简信息失败");
            }
        };
    }
}