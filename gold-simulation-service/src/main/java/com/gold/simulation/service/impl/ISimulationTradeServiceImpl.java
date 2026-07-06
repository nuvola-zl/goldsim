package com.gold.simulation.service.impl;
import com.gold.api.feign.MarketFeignApi;
import com.gold.common.code.ErrorCode;
import com.gold.common.context.UserContext;
import com.gold.common.exception.BusinessException;
import com.gold.common.mq.RabbitMqConstants;
import com.gold.simulation.domain.dto.GoldBuyDTO;
import com.gold.simulation.domain.dto.GoldSellDTO;
import com.gold.simulation.domain.dto.LeaderboardUpdateMessage;
import com.gold.simulation.domain.vo.BuyResultVO;
import com.gold.simulation.domain.vo.SellResultVO;
import com.gold.simulation.mapper.VirtualAccountMapper;
import com.gold.simulation.mapper.VirtualPositionMapper;
import com.gold.simulation.mapper.VirtualTradeRecordMapper;
import com.gold.simulation.service.ISimulationTradeService;
import com.gold.simulation.service.LeaderboardService;
import com.gold.simulation.service.TradeCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ISimulationTradeServiceImpl implements ISimulationTradeService {

    private final RedissonClient redissonClient;

    private final RabbitTemplate rabbitTemplate;

    private final TradeCoreService tradeCoreService;


    // 交易分布式锁前缀
    private static final String TRADE_LOCK_PREFIX = "sim:trade:";

    /**
     * 买入
     *
     * 【当前架构】单体事务：@Transactional 保证 account、position、trade_record 三表一致
     *
     * 【未来演进 - MQ + Seata 分布式事务】
     * 当 QPS > 1000 或团队多人开发时，拆分为三个独立服务：
     *
     *   1. simulation-account-service（虚拟账户服务）
     *      - 职责：余额校验、扣款、退款
     *      - 本地事务：UPDATE virtual_account
     *      - 成功后发 MQ：Topic = "sim.account.deducted"
     *
     *   2. simulation-position-service（持仓服务）
     *      - 消费 MQ "sim.account.deducted"
     *      - 职责：更新持仓、加权平均成本计算
     *      - 本地事务：INSERT/UPDATE virtual_position
     *      - 成功后发 MQ：Topic = "sim.position.updated"
     *      - 失败则发 MQ：Topic = "sim.account.refund"（Saga 补偿）
     *
     *   3. simulation-record-service（流水服务）
     *      - 消费 MQ "sim.position.updated"
     *      - 职责：写入交易流水
     *      - 本地事务：INSERT virtual_trade_record
     *      - 纯记录，失败不影响核心业务，可异步重试
     *
     * 【分布式事务方案对比】
     *   - Seata AT 模式：自动代理数据源，侵入小，但性能损耗 10~15%
     *   - Seata Saga 模式：手写状态机，适合长事务，复杂度高
     *   - 本地消息表（最终一致性）：在 account 表加 msg_status 字段，定时任务扫表发 MQ，无 Seata 依赖
     *
     * 【推荐演进路径】
     *   阶段1（当前）：单体 + @Transactional
     *   阶段2（用户量1万+）：本地消息表 + 定时任务发 MQ，不引入 Seata
     *   阶段3（用户量10万+）：Seata AT 模式，三服务独立部署
     *   阶段4（用户量100万+）：Seata Saga + 异步化，流水服务完全解耦
     */
    @Override
    public BuyResultVO buy(GoldBuyDTO dto) {
        Long userId = UserContext.getUserId();
        String lockKey = TRADE_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockSuccess = false;

        try {
            lockSuccess = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!lockSuccess) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统繁忙，请重试");
            }
            return tradeCoreService.doBuy(dto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.THREAD_INTERRUPTION, "交易线程中断，请稍后重试");
        } finally {
            if (lockSuccess && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 卖出
     *
     * 【已修复】leaderboardService.updateUserScore 同步改 MQ 异步
     *
     * 原问题(7-10):
     * - leaderboardService.updateUserScore 同步调用，在交易流程内更新 Redis + WebSocket 推送
     * - 拖长交易主流程响应时间，推送失败可能影响用户体验
     *
     * 修复内容(2024-07-10):
     * - 卖出成功后发送 MQ 消息到 leaderboard.exchange
     * - LeaderboardUpdateConsumer 异步消费，更新 Redis ZSet + WebSocket 推送
     * - 交易主流程与排行榜解耦，缩短响应时间
     *
     * 未来演进（保留）:
     * - 引入 RabbitMQ：扣款成功后发消息，持仓服务、流水服务异步消费
     * - 引入 Seata AT 模式：跨服务事务一致性（扣款 + 更新持仓 + 写流水）
     * - 当前阶段：单服务 + @Transactional 本地事务，满足毕业设计/练手需求
     */
    @Override
    public SellResultVO sell(GoldSellDTO dto) {
        Long userId = UserContext.getUserId();
        String lockKey = TRADE_LOCK_PREFIX + userId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockSuccess = false;

        try {
            lockSuccess = lock.tryLock(3, 15, TimeUnit.SECONDS);
            if (!lockSuccess) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "系统繁忙，请重试");
            }
          // 改成：
            SellResultVO result = tradeCoreService.doSell(dto);

           // 【优化】排行榜更新改为 MQ 异步，缩短交易主流程耗时
           // 原同步调用：leaderboardService.updateUserScore(userId, result.getTotalProfit());
            try {
                LeaderboardUpdateMessage message = new LeaderboardUpdateMessage();
                message.setUserId(userId);
                message.setTotalProfit(result.getTotalProfit());

                rabbitTemplate.convertAndSend(
                        RabbitMqConstants.LEADERBOARD_EXCHANGE,
                        RabbitMqConstants.ROUTE_KEY_LEADERBOARD_UPDATE,
                        message
                );
                log.debug("排行榜更新消息已发送，userId={}", userId);
            } catch (Exception e) {
                log.error("排行榜更新消息发送失败，userId={}", userId, e);
                // 发送失败不影响交易结果，排行榜可通过 rebuildFromDb 兜底重建
            }

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.THREAD_INTERRUPTION, "交易线程中断，请稍后重试");
        } finally {
            if (lockSuccess && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}