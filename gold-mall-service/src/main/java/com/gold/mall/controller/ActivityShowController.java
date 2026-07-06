package com.gold.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.feign.ProductFeignApi;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.vo.ActivityShowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 【修改】活动展示控制器
 * 原来直接操作 ActivityGoodsMapper
 * 现在改为通过 ProductFeignApi 远程调用 gold-product-service
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mall/activity")
public class ActivityShowController {

    private final ProductFeignApi productFeignApi;

    @GetMapping("/ongoing")
    public Result<Page<ActivityShowVO>> getOngoingActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return productFeignApi.getOngoingActivities(page, size);
    }

    @GetMapping("/upcoming")
    public Result<Page<ActivityShowVO>> getUpcomingActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return productFeignApi.getUpcomingActivities(page, size);
    }
}