package com.gold.api.feign;


import com.gold.api.domain.dto.StoreInfoDTO;
import com.gold.api.domain.dto.StoreSimpleDTO;
import com.gold.api.fallback.StoreFeignApiFallbackFactory;
import com.gold.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "gold-store-service",
        contextId = "storeFeignApi",
        path = "/store/feign",
        fallbackFactory = StoreFeignApiFallbackFactory.class)
public interface StoreFeignApi {

    /**
     * 查门店信息（订单服务调用）
     */
    @GetMapping("/{storeId}")
    Result<StoreInfoDTO> getStoreInfo(@PathVariable("storeId") Long storeId);

    /**
     * 校验门店是否营业（订单服务调用）
     */
    @GetMapping("/{storeId}/valid")
    Result<Boolean> isStoreValid(@PathVariable("storeId") Long storeId);

    // 新增：商品服务专用，获取精简门店信息
    @GetMapping("/{storeId}/simple")
    Result<StoreSimpleDTO> getStoreSimple(@PathVariable("storeId") Long storeId);
}