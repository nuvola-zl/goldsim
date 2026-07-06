package com.gold.simulation.consumer;

import com.gold.common.mq.RabbitMqConstants;
import com.gold.simulation.domain.dto.LeaderboardUpdateMessage;
import com.gold.simulation.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardUpdateConsumer {

    private final LeaderboardService leaderboardService;

    /**
     * 消费排行榜更新消息
     * 
     * 从交易主流程解耦，异步更新 Redis 排行榜 + WebSocket 推送
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(RabbitMqConstants.QUEUE_LEADERBOARD_UPDATE),
            exchange = @Exchange(RabbitMqConstants.LEADERBOARD_EXCHANGE),
            key = RabbitMqConstants.ROUTE_KEY_LEADERBOARD_UPDATE
    ))
    public void handleUpdate(LeaderboardUpdateMessage message) {
        log.debug("收到排行榜更新消息，userId={}", message.getUserId());
        leaderboardService.updateUserScore(message.getUserId(), message.getTotalProfit());
    }
}