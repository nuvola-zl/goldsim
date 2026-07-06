package com.gold.order.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayCancelConsumer {

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "order.delay.cancel.queue")
    public void consumeCancelMsg(String orderNo) {
        log.info("收到取消超时关单消息，订单号：{}", orderNo);
        // 写入Redis标记，有效期15分钟，延迟消费者读到直接跳过关闭订单
        stringRedisTemplate.opsForValue().set(
                "order:delay:cancelled:" + orderNo,
                "1",
                15,
                TimeUnit.MINUTES
        );
        // [Redis键冲突] 此方法与 handleOrderPaySuccess() 共用 Redis key "order:paid:" + orderNo。支付处理器用 trySet 做幂等，此处用裸 set 直接覆盖。
        // 如果延时取消消费者在支付处理器 trySet 之后、DB 更新之前执行，会消耗掉幂等标记导致下次回调重复处理
        // 【已修复】Redis key 冲突
        // 原问题：与 handleOrderPaySuccess 共用 "order:paid:" + orderNo
        // 修复：使用独立 key "order:delay:cancelled:" + orderNo
    }
}