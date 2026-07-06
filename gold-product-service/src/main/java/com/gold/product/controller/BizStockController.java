package com.gold.product.controller;

import com.gold.api.domain.dto.DeductActivityStockRequest;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.api.domain.dto.SpecInfoDTO;
import com.gold.common.result.Result;
import com.gold.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/product/biz-stock")
public class BizStockController {

    private final StockService stockService;

    /**
     * 订单/模拟盘查询规格极简信息
     */
    @GetMapping("/spec/{specId}")
    public Result<SpecInfoDTO> getSpecInfo(@PathVariable Long specId) {
        try {
            return Result.success(stockService.getSpecInfoById(specId));
        } catch (RuntimeException e) {
            log.error("查询规格异常：{}", e.getMessage(), e);
            return Result.error("A00001", e.getMessage());
        }
    }

    /**
     * 下单冻结库存
     */
    @PostMapping("/deduct-freeze")
    public Result<Void> freezeStock(@RequestBody DeductStockRequest request) {
        try {
            stockService.freezeStock(request);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error("A00001", e.getMessage());
        }
    }

    /**
     * 解冻基础库存
     */
    @PostMapping("/unfreeze")
    public Result<Void> unFreezeStock(@RequestBody DeductStockRequest request) {
        try {
            stockService.unFreezeStock(request);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error("A00001", e.getMessage());
        }
    }

    /**
     * 支付成功永久扣减库存
     */
    @PostMapping("/deduct")
    public Result<Void> deductStock(@RequestBody DeductStockRequest request) {
        try {
            stockService.deductStock(request);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error("A00001", e.getMessage());
        }
    }

    /**
     * 活动库存冻结
     */
    @PostMapping("/activity/deduct-freeze")
    public Result<Void> deductActivityStock(@RequestBody DeductActivityStockRequest request) {
        try {
            stockService.deductActivityStock(request);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error("A00001", e.getMessage());
        }
    }

    /**
     * 活动库存解冻
     */
    @PostMapping("/activity/unfreeze")
    public Result<Void> unFreezeActivityStock(@RequestBody DeductActivityStockRequest request) {
        try {
            stockService.unFreezeActivityStock(request);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error("A00001", e.getMessage());
        }
    }
}