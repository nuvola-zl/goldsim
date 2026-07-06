package com.gold.market.controller;

import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.common.code.ErrorCode;
import com.gold.common.result.Result;


import com.gold.market.domain.po.Kline1m;
import com.gold.market.service.MarketPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {
    private final MarketPriceService marketPriceService;

    /**
     * 获取最新金属价格
     * @param metalType
     * @return
     */
    @GetMapping("/price/latest")
    public Result<MarketPriceDTO> getLatest(@RequestParam String metalType) {
        // 【已修复】空值处理不一致
        // 原问题(todo):
        // 直接 Result.success(null)，前端无法判断是"成功无数据"还是异常
        // 与同服务 Feign 接口（MarketFeignController）处理不一致
        // 修复(2024-07-10): 增加 null 检查，无数据时返回 error，与 Feign 接口统一
        MarketPriceDTO dto = marketPriceService.getLatestPrice(metalType);
        if (dto == null) {
            return Result.error(ErrorCode.MARKET_PRICE_FETCH_FAILED, "暂无该品种行情数据");
        }
        return Result.success(dto);
    }
    /**
     * 手动拉取真实数据（测试用）
     */
    @GetMapping("/sync")
    public Result<String> manualSync() {
        marketPriceService.syncAllMetalPrice();
        return Result.success("拉取完成，请查看日志");
    }

    /**
     * 生成 Mock 数据（周末/休市时测试用）
     */
    @GetMapping("/mock")
    public Result<String> generateMock() {
        marketPriceService.generateMockPrice();
        return Result.success("Mock数据已生成");
    }


    /**
     * 获取当日全部1分钟K线（前端蜡烛图专用）
     * @param metalType 品种 AU/AG/PT
     */
    @GetMapping("/kline/today")
    public Result<List<Kline1m>> getTodayKline(@RequestParam String metalType) {
        if (!StringUtils.hasText(metalType)) {
            return Result.error("A00001","品种类型不能为空");
        }
        List<Kline1m> klineList = marketPriceService.selectTodayKline(metalType);
        return Result.success(klineList);
    }
}