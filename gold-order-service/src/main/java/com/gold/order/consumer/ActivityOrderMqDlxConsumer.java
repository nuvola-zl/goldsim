package com.gold.order.consumer;


import com.gold.common.message.ActivityOrderMessage;
import com.gold.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityOrderMqDlxConsumer {

    private final OrderService orderService;

    /**
     * 死信队列：消费失败3次后的兜底处理
     * 1. 回滚Redis库存
     * 2. 通知用户抢购失败
     */
    @RabbitListener(queues = "order.activity.create.dlx")
    public void handleDlxMessage(ActivityOrderMessage message) {
        log.error("抢购订单进入死信队列，token={}", message.getToken());

        try {
            // 回滚Redis库存
            orderService.rollbackActivityStock(message.getActivityId(), message.getSpecId(),
                    message.getUserId(), message.getBuyNum());

            // 缓存失败结果（用户轮询能查到）
            orderService.cacheActivityResult(message.getToken(), 2, null,
                    "系统繁忙，抢购失败，请稍后再试");

            // TODO: 发送通知给用户（短信/推送）

        } catch (Exception e) {
            log.error("死信队列处理失败，token={}", message.getToken(), e);
            // 这里如果还失败，需要人工介入或告警
        }
    }
}