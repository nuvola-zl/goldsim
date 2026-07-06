package com.gold.simulation.service;


import com.gold.common.result.PageResult;
import com.gold.simulation.domain.vo.*;

import java.util.List;

public interface SimulationQueryService {
    /**
     * 查询当前用户全部持仓列表
     * 接口：[新增] GET /api/simulation/position/list
     * 用途：主页面持仓提示、个人中心"我的持仓"
     */
    List<PositionVO> listPositions(Long userId);
    /**
     * 查询当前用户持仓详情
     * 接口：[新增] GET /api/simulation/position/detail
     * 用途：个人中心"持仓详情"
     */
    PositionVO getPosition(Long userId, String metalType);
    /**
     * 查询当前用户交易记录列表
     * 接口：[新增] GET /api/simulation/trade/history
     * 用途：个人中心"交易记录"、主页面"最近交易"
     */
    PageResult<TradeRecordVO> listTradeHistory(Long userId, Integer page, Integer size, String metalType, String tradeType);
    /**
     * 查询当前用户资金详情
     * 接口：[新增] GET /api/simulation/account/detail
     * 用途：个人中心"资金详情"
     */
    VirtualAccountVO getAccountInfo(Long userId);
    /**
     * 查询当前用户资金总览
     * 接口：[新增] GET /api/simulation/account/overview
     * 用途：个人中心"资金总览"
     */
    AccountOverviewVO getAccountOverview(Long userId);

    /**
     * 资金流水查询
     */
    PageResult<AccountFlowVO> listAccountFlow(Long userId, Integer page, Integer size, String tradeType);

    /**
     * 交易统计概况
     */
    TradeStatisticsVO getTradeStatistics(Long userId);

    /**
     * 持仓盈亏实时分析
     */
    List<PositionProfitVO> listPositionProfit(Long userId);

    /**
     * 单笔交易详情
     */
    TradeRecordDetailVO getTradeDetail(Long userId, String requestId);

    /**
     * 交易规则配置
     */
    SimulationConfigVO getSimulationConfig();

    /**
     * 可卖出数量
     */
    PositionAvailableVO getAvailableAmount(Long userId, String metalType);
}