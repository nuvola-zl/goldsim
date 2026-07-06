package com.gold.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.vo.ActivityShowVO;
import com.gold.product.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/mall/activity")
public class MallActivityController {

    private final ActivityService activityService;

    /** 查询进行中的活动列表 */
    @GetMapping("/ongoing")
    public Result<Page<ActivityShowVO>> getOngoingActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(activityService.getOngoingActivities(page, size));
    }

    /** 查询即将开始的活动列表 */
    @GetMapping("/upcoming")
    public Result<Page<ActivityShowVO>> getUpcomingActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(activityService.getUpcomingActivities(page, size));
    }
}