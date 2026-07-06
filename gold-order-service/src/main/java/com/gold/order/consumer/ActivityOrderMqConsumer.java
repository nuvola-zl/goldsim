package com.gold.order.consumer;


import com.gold.common.message.ActivityOrderMessage;
import com.gold.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityOrderMqConsumer {

    private final OrderService orderService;

    /**
     * 手动ACK，消费失败重试3次后转入死信队列
     */
    @RabbitListener(queues = "order.activity.create", ackMode = "MANUAL")
    public void handleActivityOrder(ActivityOrderMessage message, Channel channel, Message mqMessage) {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        try {
            orderService.asyncCreateActivityOrder(message);
            // 成功：确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("抢购订单MQ消费异常，token={}", message.getToken(), e);
            try {
                // 失败：拒绝消息，不重新入队（转入死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("MQ拒绝消息失败", ioException);
            }
        }
    }
}