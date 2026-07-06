package com.gold.order.producer;


import com.gold.api.domain.dto.OrderDelayMessage;
import com.gold.api.domain.dto.PayNoticeMessage;
import com.gold.common.message.ActivityOrderMessage;
import com.gold.common.mq.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OrderMqProducer {

    private final RabbitTemplate rabbitTemplate;

    // ====================== 1、发送即时支付提醒 ======================
    public void sendPayNotice(PayNoticeMessage msg) {
        rabbitTemplate.convertAndSend(
                "notice.exchange",
                "pay.notice",
                msg,
                message -> {
                    // 消息持久化，防止丢失
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );
    }

    // ====================== 2、发送15分钟延迟关单消息 ======================
    public void sendDelayCloseOrder(String orderNo, long delayTime, TimeUnit unit) {
        OrderDelayMessage delayMsg = new OrderDelayMessage();
        delayMsg.setOrderNo(orderNo);
        // 转为毫秒
        long delayMs = unit.toMillis(delayTime);
        rabbitTemplate.convertAndSend(
                "order.delay.exchange",
                "order.delay",
                delayMsg,
                message -> {
                    // 设置延迟毫秒
                    message.getMessageProperties().setDelayLong(delayMs);
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );
    }
    // 新增方法
    public void sendCancelDelayOrder(String orderNo) {
        rabbitTemplate.convertAndSend("order.delay.exchange", "order.delay.cancel", orderNo);
    }

    // ==================== 【新增】抢购订单 ====================

    /** 发送抢购订单创建消息 */
    public void sendActivityOrder(ActivityOrderMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.ORDER_EXCHANGE,
                RabbitMqConstants.ROUTE_KEY_ACTIVITY_CREATE,
                message,
                msg -> {  // 【新增】消息处理器
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                }
        );
        // 【已修复】消息可靠性
        // 原问题(todo):
        // 未设置 MessageDeliveryMode.PERSISTENT
        // 修复(2024-07-10): 添加消息持久化，RabbitMQ 重启后消息不丢失
    }
}