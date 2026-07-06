package com.gold.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.feign.ProductFeignApi;
import com.gold.common.result.Result;
import com.gold.mallApi.domain.dto.ActivityAddDTO;
import com.gold.mallApi.domain.dto.ActivityGoodsUpdateDTO;
import com.gold.mallApi.domain.dto.ActivityUpdateDTO;
import com.gold.mallApi.domain.vo.ActivityAdminDetailVO;
import com.gold.mallApi.domain.vo.ActivityAdminListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 【修改】管理端活动控制器
 * 原来直接操作 MallActivityMapper / MallActivityGoodsMapper / ActivityAdminMapper
 * 现在改为通过 ProductFeignApi 远程调用 gold-product-service
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/activity")
public class ActivityAdminController {

    private final ProductFeignApi productFeignApi;

    /** 分页查询活动列表 */
    @GetMapping("/list")
    public Result<Page<ActivityAdminListVO>> pageActivity(
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return productFeignApi.pageActivity(activityName, status, page, size);
    }

    /** 查询活动详情（含关联商品） */
    @GetMapping("/{activityId}")
    public Result<ActivityAdminDetailVO> getActivityDetail(@PathVariable Long activityId) {
        return productFeignApi.getActivityDetail(activityId);
    }

    /** 添加活动 */
    @PostMapping
    public Result<Long> addActivity(@RequestBody ActivityAddDTO dto) {
        return productFeignApi.addActivity(dto);
    }

    /** 更新活动（仅待开始时才能修改） */
    @PutMapping("/{activityId}")
    public Result<Void> updateActivity(@PathVariable Long activityId, @RequestBody ActivityUpdateDTO dto) {
        return productFeignApi.updateActivity(activityId, dto);
    }

    /** 取消活动（仅待开始时才能取消） */
    @PutMapping("/{activityId}/cancel")
    public Result<Void> cancelActivity(@PathVariable Long activityId) {
        return productFeignApi.cancelActivity(activityId);
    }

    /** 删除活动（仅已结束时或者已取消时才能删除） */
    @DeleteMapping("/{activityId}")
    public Result<Void> deleteActivity(@PathVariable Long activityId) {
        return productFeignApi.deleteActivity(activityId);
    }

    /** 添加活动商品 */
    @PostMapping("/{activityId}/goods")
    public Result<Void> addActivityGoods(@PathVariable Long activityId, @RequestBody ActivityGoodsUpdateDTO dto) {
        return productFeignApi.addActivityGoods(activityId, dto);
    }

    /** 更新活动商品 */
    @PutMapping("/goods/{activityGoodsId}")
    public Result<Void> updateActivityGoods(@PathVariable Long activityGoodsId, @RequestBody ActivityGoodsUpdateDTO dto) {
        return productFeignApi.updateActivityGoods(activityGoodsId, dto);
    }

    /** 删除活动商品 */
    @DeleteMapping("/goods/{activityGoodsId}")
    public Result<Void> deleteActivityGoods(@PathVariable Long activityGoodsId) {
        return productFeignApi.deleteActivityGoods(activityGoodsId);
    }
}