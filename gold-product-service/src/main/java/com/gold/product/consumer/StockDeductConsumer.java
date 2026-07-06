package com.gold.product.consumer;

import com.alibaba.fastjson2.JSON;
import com.gold.api.domain.dto.DeductStockMqMessage;
import com.gold.api.domain.dto.DeductStockRequest;
import com.gold.common.mq.RabbitMqConstants;
import com.gold.product.service.StockService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDeductConsumer {

    private final StockService stockService;

    // 【新增】Redis 用于消息幂等去重
    private final StringRedisTemplate redisTemplate;

    // 幂等 key 前缀
    private static final String DEDUP_KEY_PREFIX = "deduplicate:stock:";
    // 幂等 key 过期时间（30分钟，覆盖消息重投递窗口）
    private static final Duration DEDUP_EXPIRE = Duration.ofMinutes(30);

    /**
     * 消费库存扣减消息
     *
     * ackMode = MANUAL：手动确认，业务成功才ack，失败进死信队列
     * 为什么手动ack？自动ack的话，业务抛异常消息就丢了
     *
     * 【已修复】消息幂等控制
     *
     * 原问题(2024-07-10):
     * - 消费者在 basicAck 前崩溃，RabbitMQ 会重新投递同一条消息
     * - 重复调用 stockService.deductStock()，虽然数据库有唯一索引兜底，
     *  但每次重复消息都会走到数据库抛 DuplicateKeyException，效率低且日志刷屏
     *
     * 修复内容(2024-07-10):
     * - 在 Consumer 层增加 Redis setIfAbsent 快速幂等拦截
     * - 重复消息在进业务前就被拦住，直接 ack 跳过
     * - 业务异常时删除 Redis 幂等标记，允许消息重新消费
     * - 数据库 StockDeductRecord.bizKey 唯一索引继续兜底（双层防护）
     */
    @RabbitListener(
            queues = RabbitMqConstants.QUEUE_DEDUCT_STOCK,
            ackMode = "MANUAL"
    )
    public void handleDeductStock(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String msgId = message.getMessageProperties().getMessageId();

        // 【已修复】消息幂等：Redis 快速去重
        // 原理：第一次消费 setIfAbsent 返回 true，继续执行业务
        //       重复消费 setIfAbsent 返回 false，直接 ack 跳过
        String dedupKey = DEDUP_KEY_PREFIX + msgId;
        Boolean isNewMessage = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", DEDUP_EXPIRE);

        if (Boolean.FALSE.equals(isNewMessage)) {
            log.warn("消息已处理过，跳过重复消费，msgId={}", msgId);
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                log.error("重复消息 ack 失败，msgId={}", msgId, e);
            }
            return;
        }

        try {
            String body = new String(message.getBody());
            DeductStockMqMessage mqMessage = JSON.parseObject(body, DeductStockMqMessage.class);

            log.info("收到库存扣减消息，orderNo={}, specId={}, quantity={}",
                    mqMessage.getOrderNo(), mqMessage.getSpecId(), mqMessage.getQuantity());

            // 转成内部DTO调用Service
            DeductStockRequest request = new DeductStockRequest();
            request.setOrderNo(mqMessage.getOrderNo());
            request.setSpecId(mqMessage.getSpecId());
            request.setQuantity(mqMessage.getQuantity());

            stockService.deductStock(request);

            // 业务成功，手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("库存扣减成功，msgId={}", msgId);

        } catch (Exception e) {
            // 【关键】业务失败时删除 Redis 幂等标记，允许消息重新消费
            // 否则消息重投递后会被 Redis 拦住，永远无法成功
            redisTemplate.delete(dedupKey);
            log.error("库存扣减消费异常，msgId={}", msgId, e);

            try {
                // 业务失败，拒绝消息，不重新入队（false=false）
                // 消息会进入死信队列，由死信消费者处理
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("拒绝消息失败", ioException);
            }
        }
    }
}