package com.gold.simulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.feign.MarketFeignApi;
import com.gold.common.result.PageResult;
import com.gold.common.result.Result;
import com.gold.simulation.domain.pojo.VirtualAccount;
import com.gold.simulation.domain.pojo.VirtualPosition;
import com.gold.simulation.domain.pojo.VirtualTradeRecord;
import com.gold.simulation.domain.vo.*;
import com.gold.simulation.mapper.VirtualAccountMapper;
import com.gold.simulation.mapper.VirtualPositionMapper;
import com.gold.simulation.mapper.VirtualTradeRecordMapper;
import com.gold.simulation.service.SimulationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimulationQueryServiceImpl implements SimulationQueryService {

    private final VirtualPositionMapper positionMapper;
    private final VirtualTradeRecordMapper tradeRecordMapper;
    private final VirtualAccountMapper accountMapper;
    private final MarketFeignApi marketFeignApi;

    // 金属编码-中文映射，全局统一使用，删除原来switch方法
    private static final Map<String, String> METAL_NAME_MAP = Map.of(
            "AU", "黄金",
            "AG", "白银",
            "PT", "铂金"
    );

    // 交易配置常量
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000.00");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.001");
    private static final BigDecimal MIN_TRADE_AMOUNT = new BigDecimal("0.0001");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * 根据金属编码获取中文名称，统一工具方法
     */
    private String getMetalName(String metalType) {
        if (metalType == null || metalType.isBlank()) {
            return "";
        }
        return METAL_NAME_MAP.getOrDefault(metalType, metalType);
    }

    @Override
    public List<PositionVO> listPositions(Long userId) {
        List<VirtualPosition> positions = positionMapper.selectList(
                new LambdaQueryWrapper<VirtualPosition>()
                        .eq(VirtualPosition::getUserId, userId)
                        .orderByDesc(VirtualPosition::getUpdateTime)
        );

        return positions.stream()
                .map(this::convertToPositionVO)
                .collect(Collectors.toList());
    }

    @Override
    public PositionVO getPosition(Long userId, String metalType) {
        VirtualPosition position = positionMapper.selectByUserAndMetal(userId, metalType);
        if (position == null) {
            // 没有持仓返回空对象，前端显示 0
            return PositionVO.builder()
                    .metalType(metalType)
                    .metalName(getMetalName(metalType))
                    .amount(ZERO)
                    .avgCostPrice(ZERO)
                    .currentPrice(getCurrentPrice(metalType))
                    .floatingProfit(ZERO)
                    .floatingProfitRate("0.00%")
                    .marketValue(ZERO)
                    .build();
        }
        return convertToPositionVO(position);
    }

    @Override
    public PageResult<TradeRecordVO> listTradeHistory(Long userId, Integer page, Integer size,
                                                      String metalType, String tradeType) {
        // 分页参数
        Page<VirtualTradeRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<VirtualTradeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualTradeRecord::getUserId, userId);
        // 过滤条件，空字符串不查询
        wrapper.eq(metalType != null && !metalType.isBlank(), VirtualTradeRecord::getMetalType, metalType);
        wrapper.eq(tradeType != null && !tradeType.isBlank(), VirtualTradeRecord::getTradeType, tradeType);
        // 倒序
        wrapper.orderByDesc(VirtualTradeRecord::getCreateTime);

        Page<VirtualTradeRecord> resultPage = tradeRecordMapper.selectPage(pageParam, wrapper);

        // DO 转 VO
        List<TradeRecordVO> voList = resultPage.getRecords().stream()
                .map(this::convertToTradeRecordVO)
                .toList();

        return PageResult.build(voList, resultPage.getTotal(), page, size);
    }

    @Override
    public VirtualAccountVO getAccountInfo(Long userId) {
        VirtualAccount account = accountMapper.selectByUserId(userId);
        if (account == null) {
            return null;
        }

        // 统计交易次数
        Long tradeCount = tradeRecordMapper.selectCount(
                new LambdaQueryWrapper<VirtualTradeRecord>()
                        .eq(VirtualTradeRecord::getUserId, userId)
        );

        return VirtualAccountVO.builder()
                .userId(userId)
                .balance(account.getBalance())
                .totalProfit(account.getTotalProfit())
                .totalFee(account.getTotalFee())
                .tradeCount(tradeCount.intValue())
                .build();
    }

    @Override
    public AccountOverviewVO getAccountOverview(Long userId) {
        VirtualAccount account = accountMapper.selectByUserId(userId);
        if (account == null) {
            return null;
        }

        // 查询所有持仓并计算市值
        List<VirtualPosition> positions = positionMapper.selectList(
                new LambdaQueryWrapper<VirtualPosition>()
                        .eq(VirtualPosition::getUserId, userId)
        );

        BigDecimal positionMarketValue = ZERO;
        List<AccountOverviewVO.PositionBriefVO> briefList = positions.stream()
                .map(p -> {
                    BigDecimal price = getCurrentPrice(p.getMetalType());
                    BigDecimal mv = p.getAmount().multiply(price);
                    return AccountOverviewVO.PositionBriefVO.builder()
                            .metalType(p.getMetalType())
                            .metalName(getMetalName(p.getMetalType()))
                            .amount(p.getAmount())
                            .marketValue(mv)
                            .build();
                })
                .collect(Collectors.toList());

        for (AccountOverviewVO.PositionBriefVO b : briefList) {
            positionMarketValue = positionMarketValue.add(b.getMarketValue());
        }

        return AccountOverviewVO.builder()
                .totalAsset(account.getBalance().add(positionMarketValue))
                .balance(account.getBalance())
                .positionMarketValue(positionMarketValue)
                .totalProfit(account.getTotalProfit())
                .todayProfit(ZERO) // TODO: 配合日统计表实现
                .positions(briefList)
                .build();
    }

    // ==================== 私有转换方法 ====================

    private PositionVO convertToPositionVO(VirtualPosition position) {
        BigDecimal currentPrice = getCurrentPrice(position.getMetalType());
        BigDecimal marketValue = position.getAmount().multiply(currentPrice);
        BigDecimal cost = position.getAmount().multiply(position.getAvgCostPrice());
        BigDecimal floatingProfit = marketValue.subtract(cost);

        String profitRate;
        if (cost.compareTo(ZERO) > 0) {
            profitRate = floatingProfit.divide(cost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP) + "%";
        } else {
            profitRate = "0.00%";
        }

        return PositionVO.builder()
                .positionId(position.getId())
                .metalType(position.getMetalType())
                .metalName(getMetalName(position.getMetalType()))
                .amount(position.getAmount())
                .avgCostPrice(position.getAvgCostPrice())
                .currentPrice(currentPrice)
                .floatingProfit(floatingProfit.setScale(2, RoundingMode.HALF_UP))
                .floatingProfitRate(profitRate)
                .marketValue(marketValue.setScale(2, RoundingMode.HALF_UP))
                .createTime(position.getCreateTime())
                .updateTime(position.getUpdateTime())
                .build();
    }

    private TradeRecordVO convertToTradeRecordVO(VirtualTradeRecord record) {
        return TradeRecordVO.builder()
                .id(record.getId())
                .requestId(record.getRequestId())  // ← 新增
                .tradeType(record.getTradeType())
                .tradeTypeName("BUY".equals(record.getTradeType()) ? "买入" : "卖出")
                .metalType(record.getMetalType())
                .metalName(getMetalName(record.getMetalType()))
                .amount(record.getAmount())
                .price(record.getPrice())
                .tradeValue(record.getTradeValue())
                .fee(record.getFee())
                .realizedProfit(record.getRealizedProfit())
                .balanceBefore(record.getBalanceBefore())
                .balanceAfter(record.getBalanceAfter())
                .createTime(record.getCreateTime())
                .build();
    }

    private BigDecimal getCurrentPrice(String metalType) {
        try {
            Result<MarketPriceDTO> result = marketFeignApi.getLatestPrice(metalType);
            if (result != null && result.getData() != null && result.getData().getLatestPrice() != null) {
                return result.getData().getLatestPrice();
            }
        } catch (Exception e) {
            // 行情服务异常时返回0，避免查询失败
        }
        return ZERO;
    }

    @Override
    public PageResult<AccountFlowVO> listAccountFlow(Long userId, Integer page, Integer size, String tradeType) {
        LambdaQueryWrapper<VirtualTradeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualTradeRecord::getUserId, userId);
        if (tradeType != null && !tradeType.isBlank()) {
            wrapper.eq(VirtualTradeRecord::getTradeType, tradeType.toUpperCase());
        }
        wrapper.orderByDesc(VirtualTradeRecord::getCreateTime);

        Page<VirtualTradeRecord> recordPage = tradeRecordMapper.selectPage(new Page<>(page, size), wrapper);

        List<AccountFlowVO> list = recordPage.getRecords().stream()
                .map(record -> AccountFlowVO.builder()
                        .requestId(record.getRequestId())
                        .tradeType(record.getTradeType())
                        .metalType(record.getMetalType())
                        .metalName(getMetalName(record.getMetalType()))
                        .amount(record.getAmount())
                        .price(record.getPrice())
                        .tradeValue(record.getTradeValue())
                        .fee(record.getFee())
                        .realizedProfit(record.getRealizedProfit())
                        .balanceAfter(record.getBalanceAfter())
                        .createTime(record.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        // 统一使用PageResult.build静态方法，不再new构造器
        return PageResult.build(list, recordPage.getTotal(), page, size);
    }

    @Override
    public TradeStatisticsVO getTradeStatistics(Long userId) {
        // 查询该用户所有交易记录
        LambdaQueryWrapper<VirtualTradeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualTradeRecord::getUserId, userId);
        List<VirtualTradeRecord> records = tradeRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return TradeStatisticsVO.builder()
                    .totalTradeCount(0)
                    .buyCount(0)
                    .sellCount(0)
                    .totalBuyValue(BigDecimal.ZERO)
                    .totalSellValue(BigDecimal.ZERO)
                    .totalFee(BigDecimal.ZERO)
                    .totalRealizedProfit(BigDecimal.ZERO)
                    .avgProfitPerTrade(BigDecimal.ZERO)
                    .winRate("0%")
                    .build();
        }

        int buyCount = 0, sellCount = 0, winCount = 0;
        BigDecimal totalBuyValue = BigDecimal.ZERO;
        BigDecimal totalSellValue = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalRealizedProfit = BigDecimal.ZERO;

        for (VirtualTradeRecord r : records) {
            totalFee = totalFee.add(r.getFee() != null ? r.getFee() : BigDecimal.ZERO);
            if ("BUY".equals(r.getTradeType())) {
                buyCount++;
                totalBuyValue = totalBuyValue.add(r.getTradeValue() != null ? r.getTradeValue() : BigDecimal.ZERO);
            } else if ("SELL".equals(r.getTradeType())) {
                sellCount++;
                totalSellValue = totalSellValue.add(r.getTradeValue() != null ? r.getTradeValue() : BigDecimal.ZERO);
                BigDecimal profit = r.getRealizedProfit() != null ? r.getRealizedProfit() : BigDecimal.ZERO;
                totalRealizedProfit = totalRealizedProfit.add(profit);
                if (profit.compareTo(BigDecimal.ZERO) > 0) {
                    winCount++;
                }
            }
        }

        int totalTradeCount = buyCount + sellCount;
        BigDecimal avgProfit = sellCount > 0
                ? totalRealizedProfit.divide(new BigDecimal(sellCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String winRate = sellCount > 0
                ? new BigDecimal(winCount).multiply(new BigDecimal("100"))
                .divide(new BigDecimal(sellCount), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + "%"
                : "0%";

        return TradeStatisticsVO.builder()
                .totalTradeCount(totalTradeCount)
                .buyCount(buyCount)
                .sellCount(sellCount)
                .totalBuyValue(totalBuyValue)
                .totalSellValue(totalSellValue)
                .totalFee(totalFee)
                .totalRealizedProfit(totalRealizedProfit)
                .avgProfitPerTrade(avgProfit)
                .winRate(winRate)
                .build();
    }

    @Override
    public List<PositionProfitVO> listPositionProfit(Long userId) {
        // 1. 查持仓
        LambdaQueryWrapper<VirtualPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualPosition::getUserId, userId);
        List<VirtualPosition> positions = positionMapper.selectList(wrapper);

        if (positions.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 调用统一真实行情方法 getCurrentPrice，走Feign拉取最新价
        return positions.stream().map(pos -> {
            // 替换mock，统一调用Feign行情接口
            BigDecimal latestPrice = getCurrentPrice(pos.getMetalType());

            BigDecimal marketValue = pos.getAmount().multiply(latestPrice);
            BigDecimal floatingProfit = pos.getAmount().multiply(latestPrice.subtract(pos.getAvgCostPrice()));

            BigDecimal profitRate = pos.getAvgCostPrice().compareTo(BigDecimal.ZERO) > 0
                    ? latestPrice.subtract(pos.getAvgCostPrice())
                    .multiply(new BigDecimal("100"))
                    .divide(pos.getAvgCostPrice(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String profitRateStr = (floatingProfit.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                    + profitRate.stripTrailingZeros().toPlainString() + "%";

            return PositionProfitVO.builder()
                    .metalType(pos.getMetalType())
                    .metalName(getMetalName(pos.getMetalType()))
                    .amount(pos.getAmount())
                    .avgCostPrice(pos.getAvgCostPrice())
                    .latestPrice(latestPrice)
                    .marketValue(marketValue)
                    .floatingProfit(floatingProfit)
                    .floatingProfitRate(profitRateStr)
                    .build();
        }).collect(Collectors.toList());
    }

// ========== 直接删除下面这个mock方法，不再使用 ==========
// private BigDecimal getMockLatestPrice(String metalType) {
//     return switch (metalType) {
//         case "AU" -> new BigDecimal("723.50");
//         case "AG" -> new BigDecimal("7.85");
//         case "PT" -> new BigDecimal("228.60");
//         default -> BigDecimal.ZERO;
//     };
// }

    @Override
    public TradeRecordDetailVO getTradeDetail(Long userId, String requestId) {
        LambdaQueryWrapper<VirtualTradeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualTradeRecord::getUserId, userId)
                .eq(VirtualTradeRecord::getRequestId, requestId);
        VirtualTradeRecord record = tradeRecordMapper.selectOne(wrapper);

        if (record == null) {
            return null;
        }

        return TradeRecordDetailVO.builder()
                .requestId(record.getRequestId())
                .tradeType(record.getTradeType())
                .tradeTypeName("BUY".equals(record.getTradeType()) ? "买入" : "卖出")
                .metalType(record.getMetalType())
                .metalName(getMetalName(record.getMetalType()))
                .amount(record.getAmount())
                .price(record.getPrice())
                .tradeValue(record.getTradeValue())
                .fee(record.getFee())
                .realizedProfit(record.getRealizedProfit())
                .balanceBefore(record.getBalanceBefore())
                .balanceAfter(record.getBalanceAfter())
                .createTime(record.getCreateTime())
                .build();
    }

    @Override
    public SimulationConfigVO getSimulationConfig() {
        return SimulationConfigVO.builder()
                .initialBalance(INITIAL_BALANCE)
                .feeRate(FEE_RATE)
                .minTradeAmount(MIN_TRADE_AMOUNT)
                .supportedMetals(Arrays.asList("AU", "AG", "PT"))
                .metalList(Arrays.asList(
                        SimulationConfigVO.MetalConfig.builder().metalType("AU").metalName("黄金").unit("元/克").build(),
                        SimulationConfigVO.MetalConfig.builder().metalType("AG").metalName("白银").unit("元/克").build(),
                        SimulationConfigVO.MetalConfig.builder().metalType("PT").metalName("铂金").unit("元/克").build()
                ))
                .build();
    }

    @Override
    public PositionAvailableVO getAvailableAmount(Long userId, String metalType) {
        String upperMetal = metalType.toUpperCase();
        LambdaQueryWrapper<VirtualPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VirtualPosition::getUserId, userId)
                .eq(VirtualPosition::getMetalType, upperMetal);
        VirtualPosition position = positionMapper.selectOne(wrapper);

        if (position == null || position.getAmount() == null || position.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return PositionAvailableVO.builder()
                    .metalType(upperMetal)
                    .metalName(getMetalName(upperMetal))
                    .availableAmount(BigDecimal.ZERO)
                    .hasPosition(false)
                    .build();
        }

        return PositionAvailableVO.builder()
                .metalType(position.getMetalType())
                .metalName(getMetalName(position.getMetalType()))
                .availableAmount(position.getAmount())
                .hasPosition(true)
                .build();
    }
}