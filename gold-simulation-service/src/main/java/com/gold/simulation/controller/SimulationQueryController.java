package com.gold.simulation.controller;


import com.gold.common.context.UserContext;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.simulation.domain.vo.*;
import com.gold.simulation.service.SimulationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模拟盘查询接口
 * 提供持仓、交易记录、账户信息等查询服务
 */
@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class SimulationQueryController {

    private final SimulationQueryService queryService;

    /**
     * 查询当前用户全部持仓列表
     * 接口：[新增] GET /api/simulation/position/list
     * 用途：主页面持仓提示、个人中心"我的持仓"
     */
    @GetMapping("/position/list")
    public Result<List<PositionVO>> listPositions() {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.listPositions(userId));
    }

    /**
     * 查询单个品种持仓
     * 接口：[新增] GET /api/simulation/position/{metalType}
     * 用途：交易面板切换品种时显示该品种持仓量
     */
    @GetMapping("/position/{metalType}")
    public Result<PositionVO> getPosition(@PathVariable String metalType) {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.getPosition(userId, metalType));
    }

    /**
     * 查询交易记录（分页）
     * 接口：[新增] GET /api/simulation/trade/history?page=&size=&metalType=&tradeType=
     * 用途：个人中心"交易记录"、主页面"最近交易"
     */
    @GetMapping("/trade/history")
    public Result<PageResult<TradeRecordVO>> listTradeHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String metalType,
            @RequestParam(required = false) String tradeType) {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.listTradeHistory(userId, page, size, metalType, tradeType));
    }

    /**
     * 查询虚拟账户信息
     * 接口：[新增] GET /api/simulation/account/info
     * 用途：主页面"模拟资产"卡片显示余额、累计盈亏
     */
    @GetMapping("/account/info")
    public Result<VirtualAccountVO> getAccountInfo() {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.getAccountInfo(userId));
    }

    /**
     * 查询资产总览（余额 + 持仓市值 + 浮动盈亏）
     * 接口：[新增] GET /api/simulation/account/overview
     * 用途：主页面"总资产"大数字
     */
    @GetMapping("/account/overview")
    public Result<AccountOverviewVO> getAccountOverview() {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.getAccountOverview(userId));
    }

    /**
     * 资金流水查询（分页）
     * 接口：GET /api/simulation/account/flow?page=&size=&tradeType=
     */
    @GetMapping("/account/flow")
    public Result<PageResult<AccountFlowVO>> listAccountFlow(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String tradeType) {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.listAccountFlow(userId, page, size, tradeType));
    }

    /**
     * 交易统计概况
     * 接口：GET /api/simulation/account/statistics
     */
    @GetMapping("/account/statistics")
    public Result<TradeStatisticsVO> getTradeStatistics() {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.getTradeStatistics(userId));
    }

    /**
     * 持仓盈亏实时分析（全部品种）
     * 接口：GET /api/simulation/position/profit
     */
    @GetMapping("/position/profit")
    public Result<List<PositionProfitVO>> listPositionProfit() {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.listPositionProfit(userId));
    }

    /**
     * 单笔交易详情
     * 接口：GET /api/simulation/trade/history/{requestId}
     */
    @GetMapping("/trade/history/{requestId}")
    public Result<TradeRecordDetailVO> getTradeDetail(@PathVariable String requestId) {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.getTradeDetail(userId, requestId));
    }

    /**
     * 交易规则/配置查询
     * 接口：GET /api/simulation/config
     */
    @GetMapping("/config")
    public Result<SimulationConfigVO> getSimulationConfig() {
        return Result.success(queryService.getSimulationConfig());
    }

    /**
     * 卖出前可卖数量校验
     * 接口：GET /api/simulation/position/{metalType}/available
     */
    @GetMapping("/position/{metalType}/available")
    public Result<PositionAvailableVO> getAvailableAmount(@PathVariable String metalType) {
        Long userId = UserContext.getUserId();
        return Result.success(queryService.getAvailableAmount(userId, metalType));
    }
}