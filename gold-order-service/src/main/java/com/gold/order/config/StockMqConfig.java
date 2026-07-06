package com.gold.order.config;

import com.gold.common.mq.RabbitMqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StockMqConfig {

    @Bean
    public DirectExchange stockExchange() {
        return new DirectExchange(RabbitMqConstants.STOCK_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange stockDlxExchange() {
        return new DirectExchange("stock.dlx.exchange", true, false);
    }

    @Bean
    public Queue stockDeductQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_DEDUCT_STOCK)
                .withArgument("x-dead-letter-exchange", "stock.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "stock.deduct.dlx")
                .build();
    }

    @Bean
    public Queue stockDeductDlxQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_DEDUCT_STOCK_DLX).build();
    }

    @Bean
    public Binding stockDeductBinding() {
        return BindingBuilder.bind(stockDeductQueue())
                .to(stockExchange())
                .with(RabbitMqConstants.ROUTE_KEY_DEDUCT_STOCK);
    }

    @Bean
    public Binding stockDeductDlxBinding() {
        return BindingBuilder.bind(stockDeductDlxQueue())
                .to(stockDlxExchange())
                .with("stock.deduct.dlx");
    }
}