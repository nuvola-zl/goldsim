package com.gold.store.controller;

import com.gold.api.domain.dto.StoreInfoDTO;
import com.gold.api.domain.dto.StoreSimpleDTO;
import com.gold.api.feign.StoreFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;
import com.gold.store.domain.po.Store;
import com.gold.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/store/feign")  // 加上这个，和 Feign 接口的 path 一致
public class StoreFeignController implements StoreFeignApi {

    private final StoreMapper storeMapper;

    @Override
    @GetMapping("/{storeId}") // 必须重新声明，完整路径 /store/feign/1
    public Result<StoreInfoDTO> getStoreInfo(@PathVariable("storeId") Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            return Result.error(ErrorCode.RESOURCE_NOT_FOUND,"门店不存在");
        }

        StoreInfoDTO dto = new StoreInfoDTO();
        dto.setStoreId(store.getId());
        dto.setStoreName(store.getStoreName());
        dto.setProvince(store.getProvince());
        dto.setCity(store.getCity());
        dto.setDistrict(store.getDistrict());
        dto.setAddress(store.getAddress());
        dto.setPhone(store.getPhone());
        dto.setBusinessHours(store.getBusinessHours());
        dto.setLongitude(store.getLongitude());
        dto.setLatitude(store.getLatitude());
        dto.setCoverImage(store.getCoverImage());
        dto.setRating(store.getRating());

        return Result.success(dto);
    }

    @Override
    @GetMapping("/{storeId}/valid")  // 必须重新声明，完整路径 /store/feign/1/valid
    public Result<Boolean> isStoreValid(@PathVariable("storeId") Long storeId) {
        Store store = storeMapper.selectById(storeId);
        boolean valid = store != null && store.getStatus() == 1;
        return Result.success(valid);
    }

    @Override
    @GetMapping("/{storeId}/simple")
    public Result<StoreSimpleDTO> getStoreSimple(@PathVariable("storeId") Long storeId) {
        Store store = storeMapper.selectById(storeId);
        // 校验门店存在+营业
        if (store == null || store.getStatus() != 1) {
            return Result.error(ErrorCode.RESOURCE_NOT_FOUND,"门店不存在或已停业");
        }
        StoreSimpleDTO dto = new StoreSimpleDTO();
        dto.setStoreId(store.getId());
        dto.setStoreName(store.getStoreName());
        // 拼接完整地址
        dto.setAddress(store.getProvince() + store.getCity() + store.getDistrict() + store.getAddress());
        dto.setPhone(store.getPhone());
        dto.setBusinessHours(store.getBusinessHours());
        dto.setRating(store.getRating());
        return Result.success(dto);
    }
}