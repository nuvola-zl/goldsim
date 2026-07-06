package com.gold.simulation.controller;


import com.gold.common.result.Result;
import com.gold.simulation.domain.dto.GoldBuyDTO;
import com.gold.simulation.domain.dto.GoldSellDTO;
import com.gold.simulation.domain.vo.BuyResultVO;
import com.gold.simulation.domain.vo.SellResultVO;
import com.gold.simulation.service.ISimulationTradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/simulation/trade")
@RequiredArgsConstructor
public class SimulationTradeController {

    private final ISimulationTradeService simulationTradeService;

    /**
     * 买入
     *
     * 请求头 X-User-Id 由网关鉴权后传入
     */
    @PostMapping("/buy")
    public Result<BuyResultVO> buy(
            @Valid @RequestBody GoldBuyDTO goldBuyDTO) {
        log.info("请求买入：品种={}, 数量={}, requestId={}",
                goldBuyDTO.getMetalType(), goldBuyDTO.getAmount(), goldBuyDTO.getRequestId());
        BuyResultVO result = simulationTradeService.buy(goldBuyDTO);
        return Result.success(result);
    }
    /**
     * 卖出
     *
     * 请求头 X-User-Id 由网关鉴权后传入
     */
    @PostMapping("/sell")
    public Result<SellResultVO> sell(
            @Valid @RequestBody GoldSellDTO goldSellDTO) {
        log.info("请求卖出：品种={}, 数量={}, requestId={}",
                goldSellDTO.getMetalType(), goldSellDTO.getAmount(), goldSellDTO.getRequestId());
        SellResultVO sellResultVO = simulationTradeService.sell(goldSellDTO);
        return Result.success(sellResultVO);
    }
}