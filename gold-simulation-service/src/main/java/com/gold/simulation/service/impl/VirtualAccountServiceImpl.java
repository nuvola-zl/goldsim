package com.gold.simulation.service.impl;


import com.gold.common.mq.RabbitMqConstants;
import com.gold.simulation.domain.dto.LeaderboardUpdateMessage;
import com.gold.simulation.domain.pojo.VirtualAccount;
import com.gold.simulation.mapper.VirtualAccountMapper;
import com.gold.simulation.service.LeaderboardService;
import com.gold.simulation.service.VirtualAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualAccountServiceImpl implements VirtualAccountService {

    private final VirtualAccountMapper accountMapper;
    private final LeaderboardService leaderboardService; // 新增
    private final RabbitTemplate rabbitTemplate;

    /**
     * 初始化用户虚拟账户
     * @param userId 用户ID
     */
    public void initAccount(Long userId) {
        // 幂等：先查是否已存在
        VirtualAccount exist = accountMapper.selectByUserId(userId);
        if (exist != null) {
            return; // 已初始化，跳过
        }

        VirtualAccount account = VirtualAccount.builder()
                .userId(userId)
                .balance(new BigDecimal("100000.00"))
                .totalProfit(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .build();

        accountMapper.insert(account);
       // 改成：
       // 【优化】排行榜更新改为 MQ 异步
        try {
            LeaderboardUpdateMessage message = new LeaderboardUpdateMessage();
            message.setUserId(userId);
            message.setTotalProfit(BigDecimal.ZERO);

            rabbitTemplate.convertAndSend(
                    RabbitMqConstants.LEADERBOARD_EXCHANGE,
                    RabbitMqConstants.ROUTE_KEY_LEADERBOARD_UPDATE,
                    message
            );
        } catch (Exception e) {
            log.error("排行榜初始化消息发送失败，userId={}", userId, e);
        }
    }
}