package com.gold.simulation.service.impl;

import com.gold.api.domain.dto.MarketPriceDTO;
import com.gold.api.feign.MarketFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.context.UserContext;
import com.gold.common.exception.BusinessException;
import com.gold.common.result.Result;
import com.gold.simulation.domain.dto.GoldBuyDTO;
import com.gold.simulation.domain.dto.GoldSellDTO;
import com.gold.simulation.domain.pojo.VirtualAccount;
import com.gold.simulation.domain.pojo.VirtualPosition;
import com.gold.simulation.domain.pojo.VirtualTradeRecord;
import com.gold.simulation.domain.vo.BuyResultVO;
import com.gold.simulation.domain.vo.SellResultVO;
import com.gold.simulation.mapper.VirtualAccountMapper;
import com.gold.simulation.mapper.VirtualPositionMapper;
import com.gold.simulation.mapper.VirtualTradeRecordMapper;
import com.gold.simulation.service.TradeCoreService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeCoreServiceImpl implements TradeCoreService {

    private final VirtualAccountMapper accountMapper;
    private final VirtualPositionMapper positionMapper;
    private final VirtualTradeRecordMapper tradeRecordMapper;
    private final MarketFeignApi marketFeignApi;

    private static final BigDecimal FEE_RATE = new BigDecimal("0.001");

    /**
     * 买入事务逻辑（禁止外部直接调用，必须通过 buy() 方法进入）
     *
     * 【已修复】request_id 数据库级幂等兜底
     *
     * 原问题(2024-07-10):
     * - 仅靠 selectByRequestId 先查后插，高并发时可能同时查到 null 导致重复插入
     * - 缺少数据库唯一索引强约束
     *
     * 修复内容(2024-07-10):
     * - 数据库已建立 uk_request_id 唯一索引（virtual_trade_record 表）
     * - 在 saveTradeRecord 中捕获 DuplicateKeyException，高并发时返回友好提示
     * - 双层防护：先 SELECT 拦截大部分重复请求，唯一索引兜底极端并发
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuyResultVO doBuy(GoldBuyDTO dto) {
        Long userId = UserContext.getUserId();
        String metalType = dto.getMetalType();
        BigDecimal amount = dto.getAmount();
        String requestId = dto.getRequestId();

        TradeContext ctx = prepareTrade(requestId, userId, metalType, amount);

        BigDecimal totalCost = ctx.getTradeValue().add(ctx.getFee());
        if (ctx.getAccount().getBalance().compareTo(totalCost) < 0) {
            throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, "余额不足");
        }

        ctx.setBalanceBefore(ctx.getAccount().getBalance());
        ctx.setBalanceAfter(ctx.getAccount().getBalance().subtract(totalCost));

        ctx.getAccount().setBalance(ctx.getBalanceAfter());
        ctx.getAccount().setTotalFee(ctx.getAccount().getTotalFee().add(ctx.getFee()));
        accountMapper.updateById(ctx.getAccount());

        addPosition(ctx);
        saveTradeRecord(ctx, "BUY");

        return BuyResultVO.builder()
                .requestId(requestId)
                .metalType(metalType)
                .tradeType("BUY")
                .amount(amount)
                .price(ctx.getPrice())
                .tradeValue(ctx.getTradeValue())
                .fee(ctx.getFee())
                .totalCost(totalCost)
                .balanceAfter(ctx.getBalanceAfter())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 卖出事务逻辑（私有，禁止外部直接调用）
     *
     * 【已修复】request_id 数据库级幂等兜底
     *
     * 原问题(2024-07-10):
     * - 仅靠分布式锁拦截同一用户并发，无法拦截锁释放后重复提交相同 requestId
     * - 缺少数据库唯一索引强约束
     *
     * 修复内容(2024-07-10):
     * - 数据库已建立 uk_request_id 唯一索引（virtual_trade_record 表）
     * - 在 saveTradeRecord 中捕获 DuplicateKeyException，高并发时返回友好提示
     * - 双层防护：分布式锁拦截同一用户并发，唯一索引兜底跨用户/跨请求重复
     *
     * 【待处理】leaderboardService.updateUserScore 同步逻辑迁移至MQ异步执行
     * - 当前 updateUserScore 同步调用，在事务内更新 Redis + WebSocket 推送
     * - 需确认调用位置后改为 MQ 异步，缩短事务耗时
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SellResultVO doSell(GoldSellDTO dto) {
        Long userId = UserContext.getUserId();
        String metalType = dto.getMetalType();
        BigDecimal amount = dto.getAmount();
        String requestId = dto.getRequestId();

        TradeContext ctx = prepareTrade(requestId, userId, metalType, amount);

        VirtualPosition position = positionMapper.selectByUserAndMetal(userId, metalType);
        if (position == null || position.getAmount().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "持仓不足，无法卖出");
        }
        ctx.setPosition(position);

        BigDecimal realizedProfit = ctx.getPrice().subtract(position.getAvgCostPrice())
                .multiply(amount)
                .setScale(2, RoundingMode.HALF_UP);
        ctx.setRealizedProfit(realizedProfit);

        BigDecimal netIncome = ctx.getTradeValue().subtract(ctx.getFee());
        ctx.setBalanceBefore(ctx.getAccount().getBalance());
        ctx.setBalanceAfter(ctx.getAccount().getBalance().add(netIncome));

        ctx.getAccount().setBalance(ctx.getBalanceAfter());
        ctx.getAccount().setTotalProfit(ctx.getAccount().getTotalProfit().add(realizedProfit));
        ctx.getAccount().setTotalFee(ctx.getAccount().getTotalFee().add(ctx.getFee()));
        accountMapper.updateById(ctx.getAccount());

        reducePosition(ctx);
        saveTradeRecord(ctx, "SELL");

        return SellResultVO.builder()
                .requestId(requestId)
                .metalType(metalType)
                .tradeType("SELL")
                .amount(amount)
                .price(ctx.getPrice())
                .tradeValue(ctx.getTradeValue())
                .fee(ctx.getFee())
                .realizedProfit(realizedProfit)
                .totalProfit(ctx.getAccount().getTotalProfit())
                .netIncome(netIncome)
                .balanceAfter(ctx.getBalanceAfter())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== 私有公共方法 ====================

    private TradeContext prepareTrade(String requestId, Long userId, String metalType, BigDecimal amount) {

        Result<MarketPriceDTO> priceResult = marketFeignApi.getLatestPrice(metalType);
        if (priceResult == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "获取行情失败，请稍后重试");
        }
        if (priceResult.isError()) {
            // 【优化】降级时返回具体错误信息
            throw new BusinessException(ErrorCode.MARKET_PRICE_FETCH_FAILED,
                    priceResult.getMsg() != null ? priceResult.getMsg() : "获取行情失败，请稍后重试");
        }
        if (priceResult.getData() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "获取行情失败，请稍后重试");
        }
        MarketPriceDTO priceDTO = priceResult.getData();
        BigDecimal price = priceDTO.getLatestPrice();

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.MARKET_PRICE_FETCH_FAILED, "行情价格异常");
        }
        // 行情服务异常时的Mock数据不可交易，避免用户按不准的价格成交
        if (Boolean.TRUE.equals(priceDTO.getMockData())) {
            throw new BusinessException(ErrorCode.MARKET_PRICE_FETCH_FAILED, "行情数据暂不可用（模拟行情），请稍后交易");
        }

        BigDecimal tradeValue = amount.multiply(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = tradeValue.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        VirtualTradeRecord exist = tradeRecordMapper.selectByRequestId(requestId);
        if (exist != null) {
            log.warn("重复请求，requestId={} 已处理", requestId);
            throw new BusinessException(ErrorCode.REQUEST_PROCESSED, "请求已处理，请查询交易记录");
        }

        VirtualAccount account = accountMapper.selectByUserId(userId);
        if (account == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "账户不存在");
        }

        return TradeContext.builder()
                .requestId(requestId)
                .userId(userId)
                .metalType(metalType)
                .amount(amount)
                .price(price)
                .tradeValue(tradeValue)
                .fee(fee)
                .account(account)
                .build();
    }

    private void addPosition(TradeContext ctx) {
        VirtualPosition position = positionMapper.selectByUserAndMetal(ctx.getUserId(), ctx.getMetalType());
        if (position == null) {
            positionMapper.insert(VirtualPosition.builder()
                    .userId(ctx.getUserId())
                    .metalType(ctx.getMetalType())
                    .amount(ctx.getAmount())
                    .avgCostPrice(ctx.getPrice())
                    .build());
        } else {
            BigDecimal totalAmount = position.getAmount().add(ctx.getAmount());
            BigDecimal newAvgCost = position.getAmount().multiply(position.getAvgCostPrice())
                    .add(ctx.getAmount().multiply(ctx.getPrice()))
                    .divide(totalAmount, 2, RoundingMode.HALF_UP);
            position.setAmount(totalAmount);
            position.setAvgCostPrice(newAvgCost);
            positionMapper.updateById(position);
        }
    }

    private void reducePosition(TradeContext ctx) {
        VirtualPosition position = ctx.getPosition();
        BigDecimal newAmount = position.getAmount().subtract(ctx.getAmount());
        if (newAmount.compareTo(BigDecimal.ZERO) == 0) {
            positionMapper.deleteById(position.getId());
        } else {
            position.setAmount(newAmount);
            positionMapper.updateById(position);
        }
    }

    private void saveTradeRecord(TradeContext ctx, String tradeType) {
        VirtualTradeRecord record = VirtualTradeRecord.builder()
                .requestId(ctx.getRequestId())
                .userId(ctx.getUserId())
                .tradeType(tradeType)
                .metalType(ctx.getMetalType())
                .amount(ctx.getAmount())
                .price(ctx.getPrice())
                .tradeValue(ctx.getTradeValue())
                .fee(ctx.getFee())
                .realizedProfit(ctx.getRealizedProfit())
                .balanceBefore(ctx.getBalanceBefore())
                .balanceAfter(ctx.getBalanceAfter())
                .createTime(LocalDateTime.now())
                .build();

        try {
            tradeRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 【兜底】高并发时两个请求同时穿过 selectByRequestId，唯一索引拦截第二个
            // 此时业务逻辑（扣款、更新持仓）已经执行完，但记录插不进去
            // 直接抛"请求已处理"，让上层返回幂等结果
            log.warn("唯一索引拦截重复请求，requestId={}", ctx.getRequestId());
            throw new BusinessException(ErrorCode.REQUEST_PROCESSED, "请求已处理，请查询交易记录");
        }
    }

    // ==================== 内部类：交易上下文 ====================

    @Data
    @Builder
    private static class TradeContext {
        private String requestId;
        private Long userId;
        private String metalType;
        private BigDecimal amount;
        private BigDecimal price;
        private BigDecimal tradeValue;
        private BigDecimal fee;
        private VirtualAccount account;
        private VirtualPosition position;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private BigDecimal realizedProfit;
    }

}
