package com.gold.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.dto.ActivityAddDTO;
import com.gold.mallApi.domain.dto.ActivityGoodsUpdateDTO;
import com.gold.mallApi.domain.dto.ActivityUpdateDTO;
import com.gold.mallApi.domain.vo.ActivityAdminDetailVO;
import com.gold.mallApi.domain.vo.ActivityAdminListVO;
import com.gold.product.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/admin/activity")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/list")
    public Result<Page<ActivityAdminListVO>> pageActivity(
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(activityService.pageActivity(activityName, status, page, size));
    }

    @GetMapping("/{activityId}")
    public Result<ActivityAdminDetailVO> getActivityDetail(@PathVariable Long activityId) {
        return Result.success(activityService.getActivityDetail(activityId));
    }

    @PostMapping
    public Result<Long> addActivity(@RequestBody ActivityAddDTO dto) {
        return Result.success(activityService.addActivity(dto));
    }

    @PutMapping("/{activityId}")
    public Result<Void> updateActivity(@PathVariable Long activityId, @RequestBody ActivityUpdateDTO dto) {
        dto.setId(activityId);
        activityService.updateActivity(dto);
        return Result.success();
    }

    @PutMapping("/{activityId}/cancel")
    public Result<Void> cancelActivity(@PathVariable Long activityId) {
        activityService.cancelActivity(activityId);
        return Result.success();
    }

    @DeleteMapping("/{activityId}")
    public Result<Void> deleteActivity(@PathVariable Long activityId) {
        activityService.deleteActivity(activityId);
        return Result.success();
    }

    @PostMapping("/{activityId}/goods")
    public Result<Void> addActivityGoods(@PathVariable Long activityId, @RequestBody ActivityGoodsUpdateDTO dto) {
        activityService.addActivityGoods(activityId, dto);
        return Result.success();
    }

    @PutMapping("/goods/{activityGoodsId}")
    public Result<Void> updateActivityGoods(@PathVariable Long activityGoodsId, @RequestBody ActivityGoodsUpdateDTO dto) {
        activityService.updateActivityGoods(activityGoodsId, dto);
        return Result.success();
    }

    @DeleteMapping("/goods/{activityGoodsId}")
    public Result<Void> deleteActivityGoods(@PathVariable Long activityGoodsId) {
        activityService.deleteActivityGoods(activityGoodsId);
        return Result.success();
    }
}