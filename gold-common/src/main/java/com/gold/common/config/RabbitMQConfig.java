package com.gold.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // ===================== 用户模块（原有） =====================
    public static final String EXCHANGE_USER = "user.exchange";
    public static final String QUEUE_USER_REGISTER = "user.register.queue";
    public static final String ROUTING_KEY_REGISTER = "user.register";

    // 延迟关单死信队列
    public static final String QUEUE_ORDER_DELAY_DLX = "order.delay.dlx.queue";
    public static final String ROUTING_ORDER_DELAY_DLX = "order.delay.dlx";

    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(EXCHANGE_USER, true, false);
    }

    @Bean
    public Queue userRegisterQueue() {
        return new Queue(QUEUE_USER_REGISTER, true);
    }

    @Bean
    public Binding userRegisterBinding() {
        return BindingBuilder.bind(userRegisterQueue())
                .to(userExchange())
                .with(ROUTING_KEY_REGISTER);
    }

    // ===================== 订单支付通知 =====================
    public static final String EXCHANGE_NOTICE = "notice.exchange";
    public static final String QUEUE_PAY_NOTICE = "pay.notice.queue";
    public static final String ROUTING_PAY_NOTICE = "pay.notice";

    @Bean
    public DirectExchange noticeExchange() {
        return new DirectExchange(EXCHANGE_NOTICE, true, false);
    }

    @Bean
    public Queue payNoticeQueue() {
        return new Queue(QUEUE_PAY_NOTICE, true);
    }

    @Bean
    public Binding payNoticeBinding() {
        return BindingBuilder.bind(payNoticeQueue())
                .to(noticeExchange())
                .with(ROUTING_PAY_NOTICE);
    }

    // ===================== 订单延迟关单（延迟交换机） =====================
    public static final String EXCHANGE_ORDER_DELAY = "order.delay.exchange";
    public static final String QUEUE_ORDER_DELAY = "order.delay.queue";
    public static final String ROUTING_ORDER_DELAY = "order.delay";

    public static final String QUEUE_ORDER_DELAY_CANCEL = "order.delay.cancel.queue";
    public static final String ROUTING_ORDER_DELAY_CANCEL = "order.delay.cancel";

    @Bean
    public DirectExchange orderDelayExchange() {
        // 延迟交换机必须携带参数 x-delayed-type
        return ExchangeBuilder.directExchange(EXCHANGE_ORDER_DELAY)
                .durable(true)
                .withArgument("x-delayed-type", "direct")
                .build();
    }

    // 正常延迟关单队列
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DELAY)
                // 死信交换机、路由key，失败消息进入订单统一死信交换机
                .withArgument("x-dead-letter-exchange", ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.delay.dlx")
                .build();
    }

    // 延迟关单死信队列
    @Bean
    public Queue orderDelayDlxQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DELAY_DLX).build();
    }

    // 延迟关单死信绑定，绑定到全局订单死信交换机 orderDlxExchange
    @Bean
    public Binding orderDelayDlxBinding() {
        return BindingBuilder.bind(orderDelayDlxQueue())
                .to(orderDlxExchange())
                .with(ROUTING_ORDER_DELAY_DLX);
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(ROUTING_ORDER_DELAY);
    }

    // 取消延迟队列（支付成功清除延迟）
    @Bean
    public Queue orderDelayCancelQueue() {
        return new Queue(QUEUE_ORDER_DELAY_CANCEL, true);
    }

    @Bean
    public Binding orderDelayCancelBinding() {
        return BindingBuilder.bind(orderDelayCancelQueue())
                .to(orderDelayExchange())
                .with(ROUTING_ORDER_DELAY_CANCEL);
    }

    // ==================== 抢购订单队列（新增） ====================
    public static final String ORDER_ACTIVITY_CREATE_QUEUE = "order.activity.create";
    public static final String ORDER_ACTIVITY_CREATE_DLX_QUEUE = "order.activity.create.dlx"; // 死信队列

    // 交换机
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_DLX_EXCHANGE = "order.dlx.exchange";

    // 在 RabbitMQConfig 里加上
    public static final String ORDER_DELAY_CLOSE_QUEUE = "order.delay.close";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_DLX_EXCHANGE, true, false);
    }

    // 抢购订单队列（带死信配置）
    @Bean
    public Queue activityOrderQueue() {
        return QueueBuilder.durable(ORDER_ACTIVITY_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.activity.create.dlx")
                .withArgument("x-message-ttl", 30000) // 消息30秒未处理自动转入死信
                .build();
    }

    @Bean
    public Queue activityOrderDlxQueue() {
        return QueueBuilder.durable(ORDER_ACTIVITY_CREATE_DLX_QUEUE).build();
    }

    @Bean
    public Binding activityOrderBinding() {
        return BindingBuilder.bind(activityOrderQueue())
                .to(orderExchange())
                .with("order.activity.create");
    }

    @Bean
    public Binding activityOrderDlxBinding() {
        return BindingBuilder.bind(activityOrderDlxQueue())
                .to(orderDlxExchange())
                .with("order.activity.create.dlx");
    }

    // 延迟关单队列（已有，补充）
    @Bean
    public Queue delayCloseQueue() {
        return QueueBuilder.durable(ORDER_DELAY_CLOSE_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.delay.cancel")
                .build();
    }
}