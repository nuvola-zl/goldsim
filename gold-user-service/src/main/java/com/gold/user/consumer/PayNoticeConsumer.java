package com.gold.user.consumer;

import com.gold.api.domain.dto.PayNoticeMessage;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PayNoticeConsumer {

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "pay-notice.queue", durable = "true"),
                    exchange = @Exchange(value = "pay-notice.exchange", type = ExchangeTypes.DIRECT, durable = "true"),
                    key = "pay.notice"
            )
    )
    public void listenPayNotice(PayNoticeMessage msg) {
        // 模拟：短信/站内推送
        String content = String.format(
                "您有一笔订单待支付：%s，应付金额：%s，%d分钟内完成支付，超时自动关闭",
                msg.getGoodsName(),
                msg.getPayAmount(),
                msg.getExpireMinute()
        );
        System.out.println("发送支付提醒短信：" + content);
        // 调用短信推送工具发送
    }
}